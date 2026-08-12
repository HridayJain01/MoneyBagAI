package com.moneybags.transaction.service;

import com.moneybags.transaction.api.TransactionModels.*;
import com.moneybags.transaction.client.AccountClient;
import com.moneybags.transaction.domain.*;
import com.moneybags.transaction.entity.*;
import com.moneybags.transaction.exception.DomainException;
import com.moneybags.transaction.repository.*;
import com.moneybags.transaction.security.RequestActor;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class TransactionQueryService {
    private final TransactionRepository transactions; private final TransactionLegRepository legs; private final FundsHoldRepository holds;
    private final JournalEntryRepository journals; private final ClearingInstructionRepository clearing; private final StatusHistoryRepository history; private final AccountClient accounts;
    private final TransactionRailDetailsRepository railDetails;

    @Transactional(readOnly=true)
    public Page<TransactionView> search(String account,String reference,TransactionStatus status,PaymentRail rail,TransactionType type,BigDecimal minAmount,BigDecimal maxAmount,Instant from,Instant to,String createdBy,Pageable pageable,RequestActor actor){
        requireQueryActor(actor); Specification<Transaction> spec=(root,q,cb)->{List<Predicate> p=new ArrayList<>();
            if(actor.customerId()!=null)p.add(cb.equal(root.get("customerId"),actor.customerId()));
            if(account!=null)p.add(cb.or(cb.equal(root.get("sourceAccountId"),account),cb.equal(root.get("destinationAccountId"),account)));
            if(reference!=null)p.add(cb.equal(root.get("reference"),reference)); if(status!=null)p.add(cb.equal(root.get("status"),status)); if(rail!=null)p.add(cb.equal(root.get("rail"),rail)); if(type!=null)p.add(cb.equal(root.get("type"),type));
            if(minAmount!=null)p.add(cb.ge(root.get("amount"),minAmount));if(maxAmount!=null)p.add(cb.le(root.get("amount"),maxAmount));if(from!=null)p.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from));if(to!=null)p.add(cb.lessThan(root.get("createdAt"),to));if(createdBy!=null)p.add(cb.equal(root.get("makerUserId"),createdBy));return cb.and(p.toArray(Predicate[]::new));};
        return transactions.findAll(spec,pageable).map(this::view);
    }
    @Transactional(readOnly=true) public TransactionView byId(String id,RequestActor actor){Transaction tx=transactions.findById(id).orElseThrow(()->DomainException.notFound("TRANSACTION_NOT_FOUND","Transaction not found: "+id));authorize(tx,actor);return view(tx);}
    @Transactional(readOnly=true) public TransactionView byReference(String ref,RequestActor actor){Transaction tx=transactions.findByReference(ref).orElseThrow(()->DomainException.notFound("TRANSACTION_NOT_FOUND","Transaction not found: "+ref));authorize(tx,actor);return view(tx);}
    @Transactional(readOnly=true) public StatusView status(String id,RequestActor actor){Transaction tx=transactions.findById(id).orElseThrow(()->DomainException.notFound("TRANSACTION_NOT_FOUND","Transaction not found: "+id));authorize(tx,actor);return new StatusView(tx.getId(),tx.getReference(),tx.getStatus(),tx.getUpdatedAt());}
    @Transactional(readOnly=true) public Page<TransactionView> account(String accountId,Pageable page,RequestActor actor){requireQueryActor(actor);if(actor.customerId()!=null)accounts.context(accountId,actor.customerId());return transactions.findBySourceAccountIdOrDestinationAccountId(accountId,accountId,page).map(tx->{authorize(tx,actor);return view(tx);});}
    @Transactional(readOnly=true) public List<TransactionView> miniStatement(String accountId,int size,RequestActor actor){return account(accountId,PageRequest.of(0,Math.min(Math.max(size,1),100),Sort.by(Sort.Direction.DESC,"createdAt")),actor).getContent();}
    @Transactional(readOnly=true) public Page<TransactionView> approvals(String branch,BigDecimal min,BigDecimal max,PaymentRail rail,Pageable page,RequestActor actor){
        actor.require("TRANSACTION_APPROVE"); Specification<Transaction> spec=(root,q,cb)->{List<Predicate> p=new ArrayList<>();p.add(cb.equal(root.get("status"),TransactionStatus.PENDING_APPROVAL));
            if(branch!=null)p.add(cb.equal(root.get("branchCode"),branch));if(min!=null)p.add(cb.ge(root.get("amount"),min));if(max!=null)p.add(cb.le(root.get("amount"),max));if(rail!=null)p.add(cb.equal(root.get("rail"),rail));return cb.and(p.toArray(Predicate[]::new));};
        return transactions.findAll(spec,page).map(this::view);
    }
    private TransactionView view(Transaction tx){
        List<LegView> lv=legs.findByTransactionIdOrderBySequenceNo(tx.getId()).stream().map(l->new LegView(l.getSequenceNo(),l.getRole().name(),l.getDirection().name(),l.getAccountId(),l.getAmount(),l.getCurrency(),l.getDescription())).toList();
        HoldView hv=holds.findByTransactionId(tx.getId()).map(h->new HoldView(h.getId(),h.getExternalHoldId(),h.getAccountId(),h.getAmount(),h.getCurrency(),h.getStatus().name())).orElse(null);
        List<JournalView> jv=journals.findByTransactionIdOrderByCreatedAt(tx.getId()).stream().map(j->new JournalView(j.getReference(),j.getType(),j.getStatus().name(),j.getTotalDebit(),j.getTotalCredit(),j.getLines().stream().map(l->new JournalLineView(l.getLineNo(),l.getLedgerAccountCode(),l.getAccountId(),l.getDebit(),l.getCredit(),l.getDescription())).toList())).toList();
        ClearingView cv=clearing.findByTransactionId(tx.getId()).map(c->new ClearingView(c.getId(),c.getRail(),c.getStatus().name(),c.getExternalReference(),c.getSettlementDate(),c.getFailureReason())).orElse(null);
        RailDetailsView rd=railDetails.findById(tx.getId()).map(d->new RailDetailsView(d.getUpiAddress(),d.getChequeNumber(),d.getCardId(),d.getClientReference())).orElse(null);
        List<HistoryView> hs=history.findByTransactionIdOrderByOccurredAt(tx.getId()).stream().map(h->new HistoryView(h.getFromStatus()==null?null:h.getFromStatus().name(),h.getToStatus().name(),h.getActorId(),h.getActorSource(),h.getReason(),h.getOccurredAt())).toList();
        return new TransactionView(tx.getId(),tx.getReference(),tx.getType(),tx.getRail(),tx.getChannel(),tx.getMethod(),tx.getSourceAccountId(),tx.getDestinationAccountId(),tx.getCustomerId(),tx.getAmount(),tx.getFeeAmount(),tx.getCurrency(),tx.getStatus(),tx.getMakerUserId(),tx.getCheckerUserId(),tx.getBranchCode(),tx.getNarration(),tx.getReversalOf()==null?null:tx.getReversalOf().getId(),tx.getCorrelationId(),tx.getCreatedAt(),tx.getUpdatedAt(),tx.getCompletedAt(),lv,hv,jv,cv,rd,hs);
    }
    private void authorize(Transaction tx,RequestActor actor){requireQueryActor(actor);if(actor.customerId()!=null&&!actor.customerId().equals(tx.getCustomerId()))throw DomainException.forbidden("TRANSACTION_SCOPE_DENIED","Transaction is outside the caller's ownership scope");}
    private void requireQueryActor(RequestActor actor){if(actor.customerId()==null&&actor.employeeId()==null)throw DomainException.forbidden("QUERY_SCOPE_REQUIRED","Owner or staff scope is required");}
}
