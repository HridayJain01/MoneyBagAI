package com.moneybags.transaction.service;

import com.moneybags.transaction.domain.TransactionStatus;
import com.moneybags.transaction.entity.*;
import com.moneybags.transaction.exception.DomainException;
import com.moneybags.transaction.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class TransactionStateMachine {
    private static final Map<TransactionStatus,Set<TransactionStatus>> ALLOWED=Map.ofEntries(
            Map.entry(TransactionStatus.RECEIVED,Set.of(TransactionStatus.VALIDATED,TransactionStatus.REJECTED,TransactionStatus.CANCELLED,TransactionStatus.FAILED)),
            Map.entry(TransactionStatus.VALIDATED,Set.of(TransactionStatus.PENDING_APPROVAL,TransactionStatus.APPROVED,TransactionStatus.FUNDS_RESERVED,TransactionStatus.PROCESSING,TransactionStatus.CANCELLED,TransactionStatus.FAILED)),
            Map.entry(TransactionStatus.PENDING_APPROVAL,Set.of(TransactionStatus.APPROVED,TransactionStatus.REJECTED,TransactionStatus.CANCELLED)),
            Map.entry(TransactionStatus.APPROVED,Set.of(TransactionStatus.FUNDS_RESERVED,TransactionStatus.PROCESSING,TransactionStatus.CANCELLED,TransactionStatus.FAILED)),
            Map.entry(TransactionStatus.FUNDS_RESERVED,Set.of(TransactionStatus.PROCESSING,TransactionStatus.CANCELLED,TransactionStatus.FAILED)),
            Map.entry(TransactionStatus.PROCESSING,Set.of(TransactionStatus.PROJECTION_PENDING,TransactionStatus.SETTLED,TransactionStatus.FAILED,TransactionStatus.REVERSAL_PENDING)),
            Map.entry(TransactionStatus.PROJECTION_PENDING,Set.of(TransactionStatus.PROCESSING,TransactionStatus.SETTLED,TransactionStatus.COMPLETED,TransactionStatus.FAILED)),
            Map.entry(TransactionStatus.SETTLED,Set.of(TransactionStatus.PROJECTION_PENDING,TransactionStatus.COMPLETED,TransactionStatus.REVERSAL_PENDING)),
            Map.entry(TransactionStatus.COMPLETED,Set.of(TransactionStatus.REVERSAL_PENDING)),
            Map.entry(TransactionStatus.REVERSAL_PENDING,Set.of(TransactionStatus.REVERSED,TransactionStatus.FAILED)));
    private final StatusHistoryRepository historyRepository;
    public void initial(Transaction transaction,String actor,String source,String reason){
        historyRepository.save(history(transaction,null,transaction.getStatus(),actor,source,reason));
    }
    public void transition(Transaction transaction,TransactionStatus next,String actor,String source,String reason){
        TransactionStatus current=transaction.getStatus();
        if(!ALLOWED.getOrDefault(current,Set.of()).contains(next)) throw DomainException.conflict("INVALID_STATE_TRANSITION","Cannot transition "+current+" to "+next);
        transaction.setStatus(next); if(next==TransactionStatus.COMPLETED) transaction.setCompletedAt(Instant.now());
        historyRepository.save(history(transaction,current,next,actor,source,reason));
        log.info("transaction_status_transition transactionId={} transactionReference={} accountId={} rail={} channel={} method={} fromStatus={} toStatus={} actor={} source={} correlationId={}",transaction.getId(),transaction.getReference(),transaction.getSourceAccountId()!=null?transaction.getSourceAccountId():transaction.getDestinationAccountId(),transaction.getRail(),transaction.getChannel(),transaction.getMethod(),current,next,actor,source,transaction.getCorrelationId());
    }
    public boolean canTransition(TransactionStatus current,TransactionStatus next){return ALLOWED.getOrDefault(current,Set.of()).contains(next);}
    private TransactionStatusHistory history(Transaction transaction,TransactionStatus from,TransactionStatus to,String actor,String source,String reason){
        return TransactionStatusHistory.builder().transaction(transaction).fromStatus(from).toStatus(to).actorId(actor).actorSource(source)
                .reason(reason).correlationId(transaction.getCorrelationId()).build();
    }
}
