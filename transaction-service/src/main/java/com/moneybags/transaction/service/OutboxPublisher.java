package com.moneybags.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.transaction.client.AccountClient;
import com.moneybags.transaction.config.TransactionProperties;
import com.moneybags.transaction.domain.*;
import com.moneybags.transaction.domain.FinancialEnums.*;
import com.moneybags.transaction.entity.*;
import com.moneybags.transaction.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service @RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository events; private final TransactionRepository transactions; private final FundsHoldRepository holds;
    private final ClearingInstructionRepository clearing; private final AccountClient accounts; private final ObjectMapper mapper; private final TransactionProperties properties; private final TransactionStateMachine states;

    @Scheduled(fixedDelayString="${moneybags.transaction.outbox.fixed-delay-ms:5000}")
    @Transactional
    public void publish(){ if(!properties.getOutbox().isEnabled())return; for(OutboxEvent event:events.findDeliverable(OutboxStatus.PENDING,Instant.now(),PageRequest.of(0,properties.getOutbox().getBatchSize())))deliver(event); }
    private void deliver(OutboxEvent event){
        try{
            AccountClient.ProjectionInstruction instruction=mapper.readValue(event.getPayload(),AccountClient.ProjectionInstruction.class);
            accounts.project(event.getDeduplicationKey(),instruction);
            if("DEBIT".equals(instruction.direction())&&instruction.holdId()!=null){
                FundsHold hold=holds.findByTransactionId(instruction.transactionId()).orElse(null); if(hold!=null&&hold.getStatus()==HoldStatus.FUNDS_HELD){accounts.consume(hold.getAccountId(),hold.getExternalHoldId(),"consume:"+instruction.transactionId());hold.setStatus(HoldStatus.CONSUMED);}
            }
            event.setStatus(OutboxStatus.PUBLISHED);event.setPublishedAt(Instant.now());event.setAttempts(event.getAttempts()+1); completeIfReady(instruction.transactionId());
        }catch(Exception ex){event.setAttempts(event.getAttempts()+1);event.setLastError(ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage().substring(0,Math.min(500,ex.getMessage().length())));if(event.getAttempts()>=properties.getOutbox().getMaxAttempts())event.setStatus(OutboxStatus.FAILED);else event.setNextAttemptAt(Instant.now().plusSeconds(Math.min(300,1L<<Math.min(8,event.getAttempts()))));}
    }
    private void completeIfReady(String transactionId){
        List<OutboxEvent> aggregate=events.findByAggregateId(transactionId); if(aggregate.stream().anyMatch(e->e.getStatus()!=OutboxStatus.PUBLISHED))return;
        Transaction tx=transactions.findById(transactionId).orElse(null); if(tx==null)return;
        boolean awaitingSettlement=tx.getType().externallyCleared()&&clearing.findByTransactionId(tx.getId()).map(c->c.getStatus()!=ClearingStatus.SETTLED).orElse(false);
        if(awaitingSettlement&&tx.getStatus()==TransactionStatus.PROJECTION_PENDING)states.transition(tx,TransactionStatus.PROCESSING,"outbox-publisher","SYSTEM","Initial account projection applied; awaiting settlement");
        else if(states.canTransition(tx.getStatus(),TransactionStatus.COMPLETED)){
            states.transition(tx,TransactionStatus.COMPLETED,"outbox-publisher","SYSTEM","All account projections applied");
            if(tx.getType()==TransactionType.REVERSAL&&tx.getReversalOf()!=null&&tx.getReversalOf().getStatus()==TransactionStatus.REVERSAL_PENDING)states.transition(tx.getReversalOf(),TransactionStatus.REVERSED,"outbox-publisher","SYSTEM","Compensating transaction completed");
        }
    }
}
