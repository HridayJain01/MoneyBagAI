package com.moneybags.account.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Service-to-service contracts. These shapes are FROZEN by existing consumers --
 * transaction-service's {@code AccountClient} and {@code CardClient}. Field names and
 * types must match those records exactly.
 *
 * <p>Note that transaction-service sends only an {@code Idempotency-Key} header on these
 * calls: it has no Feign RequestInterceptor, so no actor or correlation headers arrive.
 * Correlation travels inside {@link ProjectionInstruction#correlationId()} instead.
 */
public final class InternalModels {
    private InternalModels() {
    }

    /**
     * Mirrors {@code AccountClient.AccountContext}. Exactly seven fields.
     *
     * <ul>
     *   <li>{@code status} must be the literal "ACTIVE" for a transaction to proceed.</li>
     *   <li>{@code availableBalance} must never be null -- transaction-service calls
     *       compareTo on it without a null check.</li>
     *   <li>{@code version} is a primitive long there, so it must always be present.</li>
     * </ul>
     */
    public record AccountContext(
            String accountId,
            String accountHolderId,
            String status,
            String currency,
            BigDecimal ledgerBalance,
            BigDecimal availableBalance,
            long version) {
    }

    /** Mirrors {@code AccountClient.HoldRequest}. */
    public record HoldRequest(
            String transactionId,
            @NotNull @DecimalMin("0.0001") BigDecimal amount,
            @NotBlank String currency,
            String reason) {
    }

    /**
     * Mirrors {@code AccountClient.HoldResponse}. {@code reservedAmount} must satisfy
     * {@code compareTo(requestedAmount) == 0} or transaction-service raises
     * HOLD_AMOUNT_MISMATCH. compareTo is scale-insensitive, so DECIMAL(19,4)
     * round-tripping is safe.
     */
    public record HoldResponse(
            String holdId,
            String status,
            BigDecimal reservedAmount) {
    }

    /** Mirrors {@code AccountClient.ProjectionInstruction}. */
    public record ProjectionInstruction(
            @NotBlank String eventId,
            String transactionId,
            String transactionReference,
            @NotBlank String accountId,
            @NotBlank String direction,
            @NotNull @DecimalMin("0.0001") BigDecimal amount,
            @NotBlank String currency,
            String holdId,
            String eventType,
            String correlationId) {
    }

    /**
     * Legacy direct statement context retained for compatibility. New statement reads
     * use the Account Service outbox projection rather than a source fallback.
     */
    public record StatementContext(
            String accountId,
            String customerId,
            String branchId,
            String maskedAccountNumber,
            String accountName,
            String status,
            String currency,
            BigDecimal currentBalance) {
    }

    /** Mirrors {@code CardClient.CardContext}. */
    public record CardContext(
            String cardId,
            String accountHolderId,
            String linkedAccountId,
            String status,
            String currency) {
    }

    /**
     * Pushed to statement-reporting-service's
     * {@code POST /internal/v1/statement-read-model/accounts} with header
     * {@code X-Service-Name: account-service}.
     *
     * <p>{@code sourceUpdatedAt} must be strictly increasing per account or the consumer
     * silently drops the event as stale -- so it is taken from the account row's
     * post-flush updated_at, never from Instant.now() at serialisation time.
     */
    public record AccountEvent(
            String sourceEventId,
            String accountId,
            String customerId,
            String branchId,
            String maskedAccountNumber,
            String accountName,
            String status,
            String currency,
            BigDecimal currentBalance,
            LocalDate dormantSince,
            Instant sourceUpdatedAt) {
    }

    public record AccrualRequest(
            @NotBlank String accountId,
            @NotNull LocalDate accrualDate,
            BigDecimal principalBase,
            BigDecimal rate,
            Integer dayCountBasis) {
    }
}
