package com.moneybags.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.transaction.client.AccountClient.ProjectionInstruction;
import com.moneybags.transaction.domain.FinancialEnums.OutboxStatus;
import com.moneybags.transaction.entity.*;
import com.moneybags.transaction.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper mapper;
    public void accountProjection(Transaction tx,String accountId,String direction,java.math.BigDecimal amount,String eventType,String holdId,String suffix){
        try {
            OutboxEvent event=OutboxEvent.builder().id(UUID.randomUUID().toString()).aggregateType("TRANSACTION").aggregateId(tx.getId()).eventType(eventType)
                    .deduplicationKey(tx.getId()+":"+suffix).status(OutboxStatus.PENDING).build();
            event.setPayload(mapper.writeValueAsString(new ProjectionInstruction(event.getId(),tx.getId(),tx.getReference(),accountId,direction,amount,tx.getCurrency(),holdId,eventType,tx.getCorrelationId())));
            repository.save(event);
        } catch(Exception e){throw new IllegalStateException("Could not serialize outbox event",e);}
    }
}
