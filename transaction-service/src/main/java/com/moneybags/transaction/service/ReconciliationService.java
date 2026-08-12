package com.moneybags.transaction.service;

import com.moneybags.transaction.domain.*;
import com.moneybags.transaction.domain.FinancialEnums.*;
import com.moneybags.transaction.entity.*;
import com.moneybags.transaction.exception.DomainException;
import com.moneybags.transaction.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class ReconciliationService {
    private final TransactionRepository transactions; private final FundsHoldRepository holds; private final JournalEntryRepository journals;
    private final ClearingInstructionRepository clearing; private final OutboxEventRepository outbox; private final ReconciliationExceptionRepository exceptions;
    @Transactional public int run(){int before=(int)exceptions.count();for(Transaction tx:transactions.findAll())inspect(tx);return (int)exceptions.count()-before;}
    private void inspect(Transaction tx){
        if(tx.getStatus()==TransactionStatus.COMPLETED&&!outbox.existsByAggregateIdAndEventType(tx.getId(),eventType(tx)))create("MISSING_ACCOUNT_PROJECTION","HIGH",tx,"Completed transaction has no expected outbox event");
        holds.findByTransactionId(tx.getId()).filter(h->tx.getStatus().terminal()&&h.getStatus()==HoldStatus.FUNDS_HELD).ifPresent(h->create("ORPHANED_FUNDS_HOLD","CRITICAL",tx,"Terminal transaction retains hold "+h.getExternalHoldId()));
        for(JournalEntry j:journals.findByTransactionIdOrderByCreatedAt(tx.getId())){BigDecimal dr=j.getLines().stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal cr=j.getLines().stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO,BigDecimal::add);if(dr.compareTo(cr)!=0||dr.compareTo(j.getTotalDebit())!=0||cr.compareTo(j.getTotalCredit())!=0)create("UNBALANCED_JOURNAL","CRITICAL",tx,"Journal "+j.getReference()+" header/lines do not balance");}
        clearing.findByTransactionId(tx.getId()).filter(c->tx.getStatus().terminal()&&tx.getStatus()!=TransactionStatus.FAILED&&c.getStatus()!=ClearingStatus.SETTLED).ifPresent(c->create("UNSETTLED_CLEARING","HIGH",tx,"Terminal transaction has clearing state "+c.getStatus()));
        if(outbox.findByAggregateId(tx.getId()).stream().anyMatch(e->e.getStatus()==OutboxStatus.FAILED))create("MISSING_ACCOUNT_PROJECTION","HIGH",tx,"Outbox delivery exhausted retries");
    }
    private String eventType(Transaction tx){return switch(tx.getType()){case DEPOSIT->"DEPOSIT_POSTED";case CHEQUE->"CHEQUE_CREDIT_POSTED";case WITHDRAWAL->"WITHDRAWAL_POSTED";case REVERSAL->"TRANSACTION_REVERSED";default->"PAYMENT_POSTED";};}
    private void create(String type,String severity,Transaction tx,String evidence){if(!exceptions.existsByTypeAndTransactionIdAndStatus(type,tx.getId(),ReconciliationStatus.OPEN))exceptions.save(ReconciliationException.builder().type(type).severity(severity).transaction(tx).businessReference(tx.getReference()).status(ReconciliationStatus.OPEN).evidence(evidence).build());}
    @Transactional(readOnly=true) public Page<ReconciliationException> list(ReconciliationStatus status,Pageable page){return status==null?exceptions.findAll(page):exceptions.findByStatus(status,page);}
    @Transactional(readOnly=true) public ReconciliationException get(String id){return exceptions.findById(id).orElseThrow(()->DomainException.notFound("RECONCILIATION_EXCEPTION_NOT_FOUND","Reconciliation exception not found: "+id));}
    @Transactional public ReconciliationException assign(String id,String investigator){ReconciliationException e=get(id);if(e.getStatus()==ReconciliationStatus.RESOLVED)throw DomainException.conflict("RECONCILIATION_CONFLICT","Resolved exception cannot be reassigned");e.setAssignedTo(investigator);e.setStatus(ReconciliationStatus.ASSIGNED);return e;}
    @Transactional public ReconciliationException resolve(String id,String resolution,String notes){ReconciliationException e=get(id);if(e.getStatus()==ReconciliationStatus.RESOLVED)return e;e.setResolution(resolution+"\nEvidence/notes: "+notes);e.setResolvedAt(Instant.now());e.setStatus(ReconciliationStatus.RESOLVED);return e;}
}
