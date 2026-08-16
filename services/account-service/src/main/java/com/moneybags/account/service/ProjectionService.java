package com.moneybags.account.service;

import com.moneybags.account.api.InternalModels.ProjectionInstruction;
import com.moneybags.account.entity.*;
import com.moneybags.account.repository.*;
import com.moneybags.account.support.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;

/**
 * Applies balance movements instructed by transaction-service.
 *
 * <p>This is the only path that moves ledger_balance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectionService {

    private final AccountRepository accounts;
    private final ProjectionInboxRepository inbox;
    private final BalanceHistoryRepository balanceHistory;
    private final AccountStatusHistoryRepository statusHistory;
    private final HoldService holdService;
    private final AccountEventPublisher eventPublisher;

    /**
     * Idempotent on BOTH the eventId (primary key) and the Idempotency-Key
     * (unique dedup_key). Two keys rather than one because they guard different
     * failures: eventId guards a logical replay, dedupKey guards a retried HTTP call.
     * transaction-service's publisher retries up to ten times with exponential backoff.
     *
     * <p>Runs REQUIRES_NEW so the duplicate-detection insert is committed independently
     * of any caller transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void apply(String dedupKey, ProjectionInstruction instruction) {
        String requestHash = hash(instruction);

        if (inbox.existsById(instruction.eventId())) {
            log.debug("Projection {} already applied; ignoring replay", instruction.eventId());
            return;
        }
        var byDedup = inbox.findByDedupKey(dedupKey);
        if (byDedup.isPresent()) {
            log.debug("Projection with dedup key {} already applied; ignoring retry", dedupKey);
            return;
        }

        Account account = accounts.findByIdForUpdate(instruction.accountId())
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND",
                        "No account with id " + instruction.accountId()));

        if (!account.getCurrency().equals(instruction.currency())) {
            throw ApiException.invalid("CURRENCY_MISMATCH",
                    "Account is held in " + account.getCurrency());
        }

        Direction direction = parseDirection(instruction.direction());
        BigDecimal ledgerBefore = account.getLedgerBalance();
        BigDecimal heldBefore = account.getHeldAmount();

        // ------------------------------------------------------------------
        // The hold is consumed HERE, not by the later explicit consume call.
        //
        // transaction-service's OutboxPublisher calls project() before consume(), so at
        // this moment the hold is still HELD. If we only moved the ledger, available
        // balance would stay understated by the hold amount until consume arrived -- and
        // if consume were ever lost, the account would be over-held forever. Doing it
        // here makes both orderings converge and makes the consume call a safe no-op.
        // ------------------------------------------------------------------
        if (direction == Direction.DEBIT && instruction.holdId() != null) {
            holdService.consumeWithin(account, instruction.holdId());
        }

        BigDecimal ledgerAfter = direction == Direction.DEBIT
                ? ledgerBefore.subtract(instruction.amount())
                : ledgerBefore.add(instruction.amount());
        account.setLedgerBalance(ledgerAfter);
        account.setLastActivityAt(Instant.now());

        // The automatic opening deposit activates products that require funding. A
        // normal incoming credit also wakes a dormant account.
        if (account.getStatus() == AccountStatus.PENDING_ACTIVATION
                && direction == Direction.CREDIT
                && "OPENING_DEPOSIT_POSTED".equals(instruction.eventType())) {
            recordStatusChange(account, AccountStatus.ACTIVE, "Activated by opening deposit");
            account.setStatus(AccountStatus.ACTIVE);
        } else if (account.getStatus() == AccountStatus.DORMANT && direction == Direction.CREDIT) {
            recordStatusChange(account, AccountStatus.ACTIVE, "Reactivated by incoming credit");
            account.setStatus(AccountStatus.ACTIVE);
            account.setDormantSince(null);
        }

        accounts.saveAndFlush(account);

        balanceHistory.save(BalanceHistory.builder()
                .accountId(account.getAccountId())
                .eventId(instruction.eventId())
                .transactionId(instruction.transactionId())
                .transactionReference(instruction.transactionReference())
                .direction(direction)
                .amount(instruction.amount())
                .ledgerBalanceBefore(ledgerBefore)
                .ledgerBalanceAfter(ledgerAfter)
                .heldBefore(heldBefore)
                .heldAfter(account.getHeldAmount())
                .businessDate(LocalDate.now(ZoneOffset.UTC))
                .createdAt(Instant.now())
                .build());

        try {
            inbox.save(ProjectionInbox.builder()
                    .eventId(instruction.eventId())
                    .dedupKey(dedupKey)
                    .transactionId(instruction.transactionId())
                    .accountId(instruction.accountId())
                    .direction(direction)
                    .amount(instruction.amount())
                    .currency(instruction.currency())
                    .eventType(instruction.eventType())
                    .holdId(instruction.holdId())
                    .requestHash(requestHash)
                    .correlationId(instruction.correlationId())
                    .outcome("APPLIED")
                    .appliedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent deliveries of the same event. The loser rolls back, which
            // is correct -- the winner has already applied the balance movement.
            log.debug("Concurrent delivery of projection {}; rolling back duplicate", instruction.eventId());
            throw ex;
        }

        eventPublisher.enqueueAccountEvent(account, instruction.eventType());
    }

    private Direction parseDirection(String value) {
        try {
            return Direction.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw ApiException.invalid("INVALID_DIRECTION", "Direction must be DEBIT or CREDIT");
        }
    }

    private void recordStatusChange(Account account, AccountStatus to, String reason) {
        statusHistory.save(AccountStatusHistory.builder()
                .accountId(account.getAccountId())
                .fromStatus(account.getStatus().name())
                .toStatus(to.name())
                .reason(reason)
                .source("PROJECTION")
                .changedAt(Instant.now())
                .build());
    }

    private String hash(ProjectionInstruction instruction) {
        String canonical = String.join("|",
                instruction.eventId(),
                String.valueOf(instruction.transactionId()),
                instruction.accountId(),
                instruction.direction(),
                instruction.amount().stripTrailingZeros().toPlainString(),
                instruction.currency());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }
}
