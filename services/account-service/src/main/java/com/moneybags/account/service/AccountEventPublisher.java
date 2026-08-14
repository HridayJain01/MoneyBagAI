package com.moneybags.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.account.api.InternalModels.AccountEvent;
import com.moneybags.account.entity.Account;
import com.moneybags.account.entity.AccountOutbox;
import com.moneybags.account.entity.OutboxStatus;
import com.moneybags.account.repository.AccountOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes account events to the outbox in the same transaction as the state change they
 * describe, so an event can never be published for a change that rolled back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEventPublisher {

    public static final String DESTINATION_STATEMENT = "STATEMENT";
    public static final String DESTINATION_AUDIT = "AUDIT";

    private final AccountOutboxRepository outbox;
    private final ObjectMapper objectMapper;

    public void enqueueAccountEvent(Account account, String eventType) {
        enqueue(account, eventType, DESTINATION_STATEMENT);
        enqueue(account, eventType, DESTINATION_AUDIT);
    }

    private void enqueue(Account account, String eventType, String destination) {
        AccountEvent event = new AccountEvent(
                UUID.randomUUID().toString(),
                account.getAccountId(),
                account.getCifNo(),
                account.getBranchCode(),
                account.getMaskedAccountNumber(),
                account.getAccountName(),
                account.getStatus().name(),
                account.getCurrency(),
                account.getLedgerBalance(),
                account.getDormantSince(),
                // The account row's post-flush updated_at, NOT Instant.now(). The consumer
                // drops any event whose sourceUpdatedAt is not strictly newer than the one
                // it already holds, so this value has to advance with the actual write.
                account.getUpdatedAt());

        try {
            outbox.save(AccountOutbox.builder()
                    .eventId(event.sourceEventId())
                    .aggregateType("ACCOUNT")
                    .aggregateId(account.getAccountId())
                    .eventType(eventType == null ? "ACCOUNT_UPDATED" : eventType)
                    .destination(destination)
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .attempts(0)
                    .nextAttemptAt(Instant.now())
                    .createdAt(Instant.now())
                    .build());
        } catch (JsonProcessingException ex) {
            // Never fail the business transaction for a serialisation problem in a
            // downstream projection; the account change itself is what matters.
            log.error("Could not serialise account event for {}: {}",
                    account.getAccountId(), ex.getMessage());
        }
    }
}
