package com.moneybags.transaction.service;

import com.moneybags.transaction.api.TransactionModels.*;
import com.moneybags.transaction.client.*;
import com.moneybags.transaction.domain.*;
import com.moneybags.transaction.domain.FinancialEnums.*;
import com.moneybags.transaction.entity.*;
import com.moneybags.transaction.exception.DomainException;
import com.moneybags.transaction.repository.*;
import com.moneybags.transaction.security.RequestActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class TransactionOrchestrator {
    private final TransactionRepository transactions;
    private final FundsHoldRepository holds;
    private final ClearingInstructionRepository clearing;
    private final TransactionLegRepository legs;
    private final TransactionRailDetailsRepository railDetails;
    private final AccountClient accounts;
    private final CardClient cards;
    private final LimitService limits;
    private final TransactionStateMachine states;
    private final JournalService journals;
    private final OutboxService outbox;
    private final IdempotencyService idempotency;
    private final RequestHasher hasher;

    @Transactional
    public Transaction create(TransactionType type,PaymentRail rail,CreateRequest request,String idempotencyKey,RequestActor actor){
        actor.require("TRANSACTION_CREATE"); validateShape(type,rail,request);
        LimitQuote quote=limits.validate(request.customerId(),type,rail,request.paymentChannel(),request.currency(),request.amount().add(fee(request)));
        validateAccounts(type,request);
        String operation="CREATE_"+type; var claim=idempotency.claim(actor.callerScope(),operation,idempotencyKey,hasher.hash(List.of(type,rail,request)));
        if(claim.replay()) return claim.record().getTransaction();
        Transaction tx=Transaction.builder().reference(reference(request.clientReference())).type(type).rail(rail).channel(request.paymentChannel()).method(request.paymentMethod())
                .sourceAccountId(request.sourceAccountId()).destinationAccountId(request.destinationAccountId()).customerId(request.customerId())
                .amount(request.amount()).feeAmount(fee(request)).currency(request.currency()).status(TransactionStatus.RECEIVED).makerUserId(actor.userId()).branchCode(actor.branchCode())
                .narration(request.narration()).approvalRequired(quote.approvalRequired()).correlationId(actor.correlationId()).build();
        transactions.saveAndFlush(tx); railDetails.save(TransactionRailDetails.builder().transaction(tx).upiAddress(request.upiAddress()).chequeNumber(request.chequeNumber()).cardId(request.cardId()).clientReference(request.clientReference()).build()); states.initial(tx,actor.userId(),"API","Request accepted"); states.transition(tx,TransactionStatus.VALIDATED,actor.userId(),"API","Synchronous validation completed");
        if(quote.approvalRequired()) states.transition(tx,TransactionStatus.PENDING_APPROVAL,actor.userId(),"API","Configured approval threshold reached"); else try{process(tx,actor.userId(),"API");}catch(RuntimeException failure){releaseAfterOrchestrationFailure(tx,failure);throw failure;}
        idempotency.complete(claim.record(),tx,201); return tx;
    }

    @Transactional
    public Transaction approve(String transactionId,String key,RequestActor actor){
        actor.require("TRANSACTION_APPROVE"); var claim=idempotency.claim(actor.callerScope(),"APPROVE:"+transactionId,key,hasher.hash(List.of(transactionId,"APPROVE"))); if(claim.replay())return claim.record().getTransaction(); Transaction tx=get(transactionId);
        if(tx.getStatus()!=TransactionStatus.PENDING_APPROVAL) throw DomainException.conflict("TRANSACTION_NOT_APPROVABLE","Transaction is not pending approval");
        if(tx.getMakerUserId().equals(actor.userId())) throw DomainException.forbidden("MAKER_SELF_APPROVAL","Maker cannot approve their own transaction");
        revalidate(tx); tx.setCheckerUserId(actor.userId()); tx.setApprovedAt(Instant.now()); states.transition(tx,TransactionStatus.APPROVED,actor.userId(),"CHECKER","Approved by checker"); try{process(tx,actor.userId(),"CHECKER");}catch(RuntimeException failure){releaseAfterOrchestrationFailure(tx,failure);throw failure;} idempotency.complete(claim.record(),tx,200); return tx;
    }
    @Transactional
    public Transaction reject(String transactionId,String reason,String key,RequestActor actor){
        actor.require("TRANSACTION_APPROVE"); var claim=idempotency.claim(actor.callerScope(),"REJECT:"+transactionId,key,hasher.hash(List.of(transactionId,reason))); if(claim.replay())return claim.record().getTransaction(); Transaction tx=get(transactionId);
        if(tx.getStatus()!=TransactionStatus.PENDING_APPROVAL) throw DomainException.conflict("TRANSACTION_NOT_REJECTABLE","Transaction is not pending approval");
        if(tx.getMakerUserId().equals(actor.userId())) throw DomainException.forbidden("MAKER_SELF_APPROVAL","Maker cannot act as checker on their own transaction");
        tx.setCheckerUserId(actor.userId()); tx.setRejectionReason(requireReason(reason)); states.transition(tx,TransactionStatus.REJECTED,actor.userId(),"CHECKER",reason); idempotency.complete(claim.record(),tx,200); return tx;
    }
    @Transactional
    public Transaction cancel(String transactionId,String reason,String key,RequestActor actor){
        var claim=idempotency.claim(actor.callerScope(),"CANCEL:"+transactionId,key,hasher.hash(List.of(transactionId,reason))); if(claim.replay())return claim.record().getTransaction(); Transaction tx=get(transactionId); if(!tx.getMakerUserId().equals(actor.userId())) throw DomainException.forbidden("ONLY_MAKER_CAN_CANCEL","Only the maker can cancel this transaction");
        if(Set.of(TransactionStatus.RECEIVED,TransactionStatus.VALIDATED,TransactionStatus.PENDING_APPROVAL,TransactionStatus.APPROVED).contains(tx.getStatus())){
            states.transition(tx,TransactionStatus.CANCELLED,actor.userId(),"API",requireReason(reason)); idempotency.complete(claim.record(),tx,200); return tx;
        }
        if(tx.getStatus()==TransactionStatus.FUNDS_RESERVED){ releaseHold(tx,"cancel"); states.transition(tx,TransactionStatus.CANCELLED,actor.userId(),"API",requireReason(reason)); idempotency.complete(claim.record(),tx,200); return tx; }
        throw DomainException.conflict("TRANSACTION_NOT_CANCELLABLE","Financial processing has already started");
    }
    @Transactional
    public Transaction reverse(String transactionId,String reason,String idempotencyKey,RequestActor actor){
        actor.require("TRANSACTION_REVERSE"); Transaction original=get(transactionId);
        if(original.getStatus()!=TransactionStatus.COMPLETED) throw DomainException.conflict("TRANSACTION_NOT_REVERSIBLE","Only a completed transaction can be reversed");
        if(original.getType()==TransactionType.REVERSAL) throw DomainException.conflict("TRANSACTION_NOT_REVERSIBLE","A reversal cannot itself be reversed");
        if(transactions.existsByReversalOfId(original.getId())) throw DomainException.conflict("TRANSACTION_ALREADY_REVERSED","A reversal already exists");
        var claim=idempotency.claim(actor.callerScope(),"REVERSE:"+original.getId(),idempotencyKey,hasher.hash(List.of(original.getId(),reason)));
        if(claim.replay()) return claim.record().getTransaction();
        states.transition(original,TransactionStatus.REVERSAL_PENDING,actor.userId(),"API",reason);
        Transaction reversal=Transaction.builder().reference(reference(null)).type(TransactionType.REVERSAL).rail(original.getRail()).channel(PaymentChannel.INTERNAL).method(PaymentMethod.ACCOUNT)
                .sourceAccountId(original.getSourceAccountId()).destinationAccountId(original.getDestinationAccountId()).customerId(original.getCustomerId())
                .amount(original.getAmount()).feeAmount(original.getFeeAmount()).currency(original.getCurrency()).status(TransactionStatus.RECEIVED).makerUserId(actor.userId()).narration("Reversal of "+original.getReference()+": "+reason)
                .approvalRequired(false).reversalOf(original).correlationId(actor.correlationId()).build();
        transactions.saveAndFlush(reversal); states.initial(reversal,actor.userId(),"API","Reversal requested"); states.transition(reversal,TransactionStatus.VALIDATED,actor.userId(),"API","Original transaction is reversible");
        reserveForReversalIfNeeded(reversal,original); states.transition(reversal,TransactionStatus.PROCESSING,actor.userId(),"API","Compensating records created");
        journals.createReversalLegs(reversal,original); journals.createReversalJournal(reversal,original); createReversalOutbox(reversal,original); states.transition(reversal,TransactionStatus.PROJECTION_PENDING,actor.userId(),"API","Compensating projections queued");
        idempotency.complete(claim.record(),reversal,201); return reversal;
    }
    private void process(Transaction tx,String actor,String source){
        if(tx.getType().debitsCustomer()) reserve(tx,tx.getSourceAccountId(),tx.totalDebit());
        states.transition(tx,TransactionStatus.PROCESSING,actor,source,"Financial processing started"); journals.createInitialFinancialFacts(tx);
        if(tx.getType().externallyCleared()) clearing.save(ClearingInstruction.builder().transaction(tx).rail(tx.getRail()).status(ClearingStatus.CREATED).amount(tx.getAmount()).currency(tx.getCurrency()).build());
        if(tx.getType()==TransactionType.CHEQUE) return;
        if(tx.getType()==TransactionType.DEPOSIT) outbox.accountProjection(tx,tx.getDestinationAccountId(),"CREDIT",tx.getAmount(),"DEPOSIT_POSTED",null,"deposit-credit");
        else {
            String hold=holds.findByTransactionId(tx.getId()).map(FundsHold::getExternalHoldId).orElse(null);
            outbox.accountProjection(tx,tx.getSourceAccountId(),"DEBIT",tx.totalDebit(),tx.getType()==TransactionType.WITHDRAWAL?"WITHDRAWAL_POSTED":"PAYMENT_POSTED",hold,"source-debit");
            if(tx.getType()==TransactionType.INTERNAL_TRANSFER){ journals.createSettlementJournal(tx); outbox.accountProjection(tx,tx.getDestinationAccountId(),"CREDIT",tx.getAmount(),"CREDIT_POSTED",null,"destination-credit"); }
        }
        states.transition(tx,TransactionStatus.PROJECTION_PENDING,actor,source,"Account projections queued in transactional outbox");
    }
    private void reserve(Transaction tx,String accountId,BigDecimal amount){
        String key="hold:"+tx.getId(); AccountClient.HoldResponse response=accounts.reserve(accountId,key,new AccountClient.HoldRequest(tx.getId(),amount,tx.getCurrency(),tx.getType().name()));
        if(response.reservedAmount()==null||response.reservedAmount().compareTo(amount)!=0) throw DomainException.conflict("HOLD_AMOUNT_MISMATCH","Account Service did not reserve the requested amount");
        holds.save(FundsHold.builder().transaction(tx).accountId(accountId).externalHoldId(response.holdId()).amount(amount).currency(tx.getCurrency()).status(HoldStatus.FUNDS_HELD).operationKey(key).build());
        states.transition(tx,TransactionStatus.FUNDS_RESERVED,"account-service","ACCOUNT_SERVICE","Funds reserved atomically by Account Service");
    }
    private void releaseHold(Transaction tx,String reason){ holds.findByTransactionId(tx.getId()).filter(h->h.getStatus()==HoldStatus.FUNDS_HELD).ifPresent(h->{accounts.release(h.getAccountId(),h.getExternalHoldId(),"release:"+tx.getId()+":"+reason);h.setStatus(HoldStatus.RELEASED);}); }
    private void releaseAfterOrchestrationFailure(Transaction tx,RuntimeException failure){
        try{releaseHold(tx,"orchestration-failure");}catch(Exception releaseFailure){log.error("hold_compensation_failed transactionId={} transactionReference={} correlationId={}",tx.getId(),tx.getReference(),tx.getCorrelationId(),releaseFailure);failure.addSuppressed(releaseFailure);}
    }
    private void validateAccounts(TransactionType type,CreateRequest r){
        if(type==TransactionType.DEPOSIT||type==TransactionType.CHEQUE){activeAccount(r.destinationAccountId(),r.customerId(),r.currency(),false);return;}
        AccountClient.AccountContext source=activeAccount(r.sourceAccountId(),r.customerId(),r.currency(),true);
        if(source.availableBalance().compareTo(r.amount().add(fee(r)))<0) throw DomainException.conflict("INSUFFICIENT_FUNDS","Available balance is insufficient for amount plus fee");
        if(type==TransactionType.INTERNAL_TRANSFER) activeAccount(r.destinationAccountId(),r.customerId(),r.currency(),false);
        if(type==TransactionType.CARD_PAYMENT){CardClient.CardContext card=cards.context(r.cardId(),r.customerId());if(!"ACTIVE".equals(card.status()))throw DomainException.conflict("CARD_INACTIVE","Linked card is not active");if(!r.customerId().equals(card.customerId())||!r.sourceAccountId().equals(card.linkedAccountId()))throw DomainException.forbidden("CARD_NOT_LINKED","Card is not linked to the requesting customer and source account");if(!r.currency().equals(card.currency()))throw DomainException.invalid("CURRENCY_MISMATCH","Transaction currency does not match card currency");}
    }
    private AccountClient.AccountContext activeAccount(String id,String customer,String currency,boolean requireOwner){
        if(id==null||id.isBlank()) throw DomainException.invalid("ACCOUNT_REQUIRED","Required account ID is missing"); AccountClient.AccountContext context=accounts.context(id,customer);
        if(!"ACTIVE".equals(context.status())) throw DomainException.conflict("ACCOUNT_INACTIVE","Account is not active: "+id);
        if(requireOwner&&!customer.equals(context.customerId())) throw DomainException.forbidden("ACCOUNT_NOT_OWNED","Source account does not belong to the requesting customer");
        if(!currency.equals(context.currency())) throw DomainException.invalid("CURRENCY_MISMATCH","Transaction currency does not match account currency"); return context;
    }
    private void revalidate(Transaction tx){
        CreateRequest r=new CreateRequest(tx.getSourceAccountId(),tx.getDestinationAccountId(),tx.getCustomerId(),null,tx.getAmount(),tx.getFeeAmount(),tx.getCurrency(),tx.getChannel(),tx.getMethod(),null,null,tx.getNarration(),tx.getReference());
        limits.validate(tx.getCustomerId(),tx.getType(),tx.getRail(),tx.getChannel(),tx.getCurrency(),tx.totalDebit()); validateAccounts(tx.getType(),r);
    }
    private void validateShape(TransactionType type,PaymentRail rail,CreateRequest r){
        PaymentRail expected=switch(type){case DEPOSIT,WITHDRAWAL->PaymentRail.CASH;case INTERNAL_TRANSFER->PaymentRail.INTERNAL;case NEFT->PaymentRail.NEFT;case RTGS->PaymentRail.RTGS;case IMPS->PaymentRail.IMPS;case UPI->PaymentRail.UPI;case CHEQUE->PaymentRail.CHEQUE;case CARD_PAYMENT->PaymentRail.CARD;case REVERSAL->PaymentRail.INTERNAL;};
        if(rail!=expected) throw DomainException.invalid("UNSUPPORTED_RAIL","Transaction type and rail do not match");
        if(r.paymentMethod().name().equals(rail.name())==false&&!(rail==PaymentRail.INTERNAL&&r.paymentMethod()==PaymentMethod.ACCOUNT)&&!(rail==PaymentRail.CASH&&r.paymentMethod()==PaymentMethod.CASH)) throw DomainException.invalid("INVALID_PAYMENT_METHOD","Payment method does not match the selected rail");
        if(r.sourceAccountId()!=null&&r.sourceAccountId().equals(r.destinationAccountId())) throw DomainException.invalid("SAME_ACCOUNT_TRANSFER","Source and destination accounts must differ");
        if(type==TransactionType.UPI&&(r.upiAddress()==null||r.upiAddress().isBlank())) throw DomainException.invalid("UPI_ADDRESS_REQUIRED","UPI address is required");
        if(type==TransactionType.CHEQUE&&(r.chequeNumber()==null||r.chequeNumber().isBlank())) throw DomainException.invalid("CHEQUE_NUMBER_REQUIRED","Cheque number is required");
        if(type==TransactionType.CARD_PAYMENT&&(r.cardId()==null||r.cardId().isBlank())) throw DomainException.invalid("CARD_ID_REQUIRED","Linked card ID is required");
    }
    private void reserveForReversalIfNeeded(Transaction reversal,Transaction original){
        if(original.getType()==TransactionType.DEPOSIT) reserve(reversal,original.getDestinationAccountId(),original.getAmount());
        else if(original.getType()==TransactionType.INTERNAL_TRANSFER) reserve(reversal,original.getDestinationAccountId(),original.getAmount());
    }
    private void createReversalOutbox(Transaction reversal,Transaction original){
        List<TransactionLeg> originalLegs=legs.findByTransactionIdOrderBySequenceNo(original.getId()); int n=0;
        for(TransactionLeg leg:originalLegs){ if(leg.getAccountId()!=null) outbox.accountProjection(reversal,leg.getAccountId(),leg.getDirection()==Direction.DEBIT?"CREDIT":"DEBIT",leg.getAmount(),"TRANSACTION_REVERSED",holds.findByTransactionId(reversal.getId()).map(FundsHold::getExternalHoldId).orElse(null),"reversal-"+(++n)); }
    }
    public Transaction get(String id){return transactions.findById(id).orElseThrow(()->DomainException.notFound("TRANSACTION_NOT_FOUND","Transaction not found: "+id));}
    private BigDecimal fee(CreateRequest r){return r.feeAmount()==null?BigDecimal.ZERO:r.feeAmount();}
    private String requireReason(String reason){if(reason==null||reason.isBlank())throw DomainException.invalid("REASON_REQUIRED","A reason is required");return reason;}
    private String reference(String client){return client!=null&&!client.isBlank()?client:"TXN-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase();}
}
