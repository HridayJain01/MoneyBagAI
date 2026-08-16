package com.moneybags.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.transaction.client.AccountClient;
import com.moneybags.transaction.client.AccountClient.ProjectionInstruction;
import com.moneybags.transaction.client.LedgerClient;
import com.moneybags.transaction.client.StatementClient;
import com.moneybags.transaction.domain.FinancialEnums.OutboxStatus;
import com.moneybags.transaction.entity.*;
import com.moneybags.transaction.repository.JournalEntryRepository;
import com.moneybags.transaction.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class OutboxService {
    public static final String LEDGER_POST = "LEDGER_POST";
    public static final String STATEMENT_POST = "STATEMENT_POST";

    private final OutboxEventRepository repository;
    private final JournalEntryRepository journals;
    private final LedgerPostingCatalog ledgerCatalog;
    private final ObjectMapper mapper;
    public void accountProjection(Transaction tx,String accountId,String direction,java.math.BigDecimal amount,String eventType,String holdId,String suffix){
        try {
            OutboxEvent event=OutboxEvent.builder().id(UUID.randomUUID().toString()).aggregateType("TRANSACTION").aggregateId(tx.getId()).eventType(eventType)
                    .deduplicationKey(tx.getId()+":"+suffix).status(OutboxStatus.PENDING).build();
            event.setPayload(mapper.writeValueAsString(new ProjectionInstruction(event.getId(),tx.getId(),tx.getReference(),accountId,direction,amount,tx.getCurrency(),holdId,eventType,tx.getCorrelationId())));
            repository.save(event);
        } catch(Exception e){throw new IllegalStateException("Could not serialize outbox event",e);}
    }

    public void ledgerPostings(Transaction tx) {
        var financialFacts = journals.findByTransactionIdOrderByCreatedAt(tx.getId());
        if (financialFacts.isEmpty()) {
            throw new IllegalStateException("No balanced journal exists for transaction " + tx.getId());
        }
        for (JournalEntry journal : financialFacts) {
            String deduplicationKey = tx.getId() + ":ledger:" + journal.getReference();
            if (repository.existsByDeduplicationKey(deduplicationKey)) continue;
            enqueue(tx, LEDGER_POST, deduplicationKey, ledgerCatalog.request(tx, journal));
        }
    }

    public void statementProjection(Transaction tx, LedgerClient.JournalLineResponse line,
                                    AccountClient.AccountContext account, Instant postedAt,
                                    BigDecimal amount, BigDecimal feeAmount) {
        String deduplicationKey = tx.getId() + ":statement:" + line.id();
        if (repository.existsByDeduplicationKey(deduplicationKey)) return;
        String eventId = UUID.randomUUID().toString();
        StatementClient.TransactionEvent payload = new StatementClient.TransactionEvent(
                eventId, tx.getId(), line.id().toString(), tx.getReference(),
                line.customerAccountId(), account.accountHolderId(), tx.getBranchCode(), line.side(),
                amount, feeAmount, tx.getCurrency(), tx.getType().name(), "COMPLETED",
                tx.getNarration(), tx.getReversalOf() == null ? null : tx.getReversalOf().getId(),
                postedAt, account.ledgerBalance(), Instant.now());
        enqueue(eventId, tx, STATEMENT_POST, deduplicationKey, payload);
    }

    private void enqueue(Transaction tx, String eventType, String deduplicationKey, Object payload) {
        enqueue(UUID.randomUUID().toString(), tx, eventType, deduplicationKey, payload);
    }

    private void enqueue(String eventId, Transaction tx, String eventType,
                         String deduplicationKey, Object payload) {
        try {
            repository.save(OutboxEvent.builder().id(eventId).aggregateType("TRANSACTION")
                    .aggregateId(tx.getId()).eventType(eventType).deduplicationKey(deduplicationKey)
                    .payload(mapper.writeValueAsString(payload)).status(OutboxStatus.PENDING).build());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize " + eventType + " outbox event", exception);
        }
    }
}
