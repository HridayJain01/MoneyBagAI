package com.moneybags.account.service;

import com.moneybags.account.api.InternalModels.HoldRequest;
import com.moneybags.account.api.InternalModels.HoldResponse;
import com.moneybags.account.entity.*;
import com.moneybags.account.repository.AccountRepository;
import com.moneybags.account.repository.FundsHoldRepository;
import com.moneybags.account.support.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Funds holds placed by transaction-service while a transaction is in flight.
 *
 * <p>A hold reduces AVAILABLE balance but never touches ledger_balance. Only an applied
 * projection moves the ledger, which is what keeps this service consistent with
 * transaction-service's journals -- the accounting facts of record.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoldService {

    private final AccountRepository accounts;
    private final FundsHoldRepository holds;

    /**
     * Places a hold, or returns the existing one on a retry.
     *
     * <p>Idempotency is on transaction_id (UNIQUE in the schema) rather than only on the
     * Idempotency-Key, so a retry that regenerates the key still cannot double-hold.
     */
    @Transactional
    public HoldResponse reserve(String accountId, HoldRequest request) {
        if (request.transactionId() != null) {
            var existing = holds.findByTransactionId(request.transactionId());
            if (existing.isPresent()) {
                FundsHold hold = existing.get();
                return new HoldResponse(hold.getHoldId(), hold.getStatus().name(), hold.getAmount());
            }
        }

        // FOR UPDATE. Serialises concurrent holds on this account so the available-balance
        // check and the held_amount increment cannot interleave.
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND",
                        "No account with id " + accountId));

        // Re-checked here even though transaction-service pre-checks the context: the
        // account can change between that read and this write.
        if (!account.getStatus().allowsDebit()) {
            throw ApiException.unprocessable("ACCOUNT_NOT_ACTIVE",
                    "Account is " + account.getStatus() + " and cannot be debited");
        }
        if (!account.getCurrency().equals(request.currency())) {
            throw ApiException.invalid("CURRENCY_MISMATCH",
                    "Account is held in " + account.getCurrency());
        }

        BigDecimal available = account.availableBalance();
        if (available.compareTo(request.amount()) < 0) {
            // 409 here means insufficient funds and nothing else -- see ApiException.conflict.
            throw ApiException.conflict("INSUFFICIENT_FUNDS",
                    "Available balance " + available + " is less than " + request.amount());
        }

        FundsHold hold = FundsHold.builder()
                .holdId(UUID.randomUUID().toString())
                .accountId(accountId)
                .transactionId(request.transactionId())
                .amount(request.amount())
                .currency(request.currency())
                .reason(request.reason() == null ? "TRANSACTION" : request.reason())
                .holdType(HoldType.TRANSACTION)
                .status(HoldStatus.HELD)
                .placedBy("SERVICE:transaction-service")
                .createdAt(Instant.now())
                .build();
        holds.save(hold);

        account.setHeldAmount(account.getHeldAmount().add(request.amount()));
        accounts.save(account);

        // reservedAmount is echoed back exactly as requested; the caller compares with
        // compareTo, so scale does not matter but the value must be identical.
        return new HoldResponse(hold.getHoldId(), hold.getStatus().name(), hold.getAmount());
    }

    /**
     * Marks a hold consumed.
     *
     * <p>Usually a no-op: transaction-service's OutboxPublisher calls project() BEFORE
     * consume(), and the projection handler already consumes the hold in the same
     * transaction as the debit. Keeping this endpoint idempotent means losing the
     * consume call entirely is harmless.
     */
    @Transactional
    public void consume(String accountId, String holdId) {
        FundsHold hold = requireHold(accountId, holdId);
        if (hold.getStatus() == HoldStatus.CONSUMED) {
            return;
        }
        if (hold.getStatus() != HoldStatus.HELD) {
            log.warn("Consume requested for hold {} in state {}; ignoring", holdId, hold.getStatus());
            return;
        }
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "No account " + accountId));
        releaseHeldAmount(account, hold.getAmount());
        hold.setStatus(HoldStatus.CONSUMED);
        hold.setConsumedAt(Instant.now());
        holds.save(hold);
        accounts.save(account);
    }

    /**
     * Releases a hold and returns the funds to available balance.
     *
     * <p>Idempotent on the HOLD STATE, not just on the key. transaction-service builds
     * release keys as {@code release:<txId>:<reason>} where reason varies between
     * "cancel" and "orchestration-failure", so two distinct keys can legitimately target
     * the same hold. This must never return 4xx: the caller treats an exception here as
     * a failed compensation.
     */
    @Transactional
    public void release(String accountId, String holdId, String reason) {
        FundsHold hold = requireHold(accountId, holdId);
        if (hold.getStatus() == HoldStatus.RELEASED) {
            return;
        }
        if (hold.getStatus() == HoldStatus.CONSUMED) {
            // A real anomaly worth surfacing, but not worth failing a compensation over.
            log.warn("Release requested for already-consumed hold {} (reason: {})", holdId, reason);
            return;
        }
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "No account " + accountId));
        releaseHeldAmount(account, hold.getAmount());
        hold.setStatus(HoldStatus.RELEASED);
        hold.setReleasedAt(Instant.now());
        hold.setReleaseReason(reason);
        holds.save(hold);
        accounts.save(account);
    }

    /**
     * Consumes a hold as part of applying a debit projection. Called by
     * ProjectionService inside the same transaction as the ledger movement.
     *
     * @return the amount removed from held_amount, or zero if the hold was not HELD
     */
    BigDecimal consumeWithin(Account account, String holdId) {
        if (holdId == null) {
            return BigDecimal.ZERO;
        }
        FundsHold hold = holds.findById(holdId).orElse(null);
        if (hold == null || hold.getStatus() != HoldStatus.HELD) {
            return BigDecimal.ZERO;
        }
        hold.setStatus(HoldStatus.CONSUMED);
        hold.setConsumedAt(Instant.now());
        holds.save(hold);
        releaseHeldAmount(account, hold.getAmount());
        return hold.getAmount();
    }

    /** Clamped at zero so a double-release can never drive held_amount negative. */
    private void releaseHeldAmount(Account account, BigDecimal amount) {
        BigDecimal remaining = account.getHeldAmount().subtract(amount);
        account.setHeldAmount(remaining.max(BigDecimal.ZERO));
    }

    private FundsHold requireHold(String accountId, String holdId) {
        FundsHold hold = holds.findById(holdId)
                .orElseThrow(() -> ApiException.notFound("HOLD_NOT_FOUND", "No hold with id " + holdId));
        if (!hold.getAccountId().equals(accountId)) {
            throw ApiException.invalid("HOLD_ACCOUNT_MISMATCH", "Hold does not belong to this account");
        }
        return hold;
    }
}
