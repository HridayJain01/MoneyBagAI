package com.moneybags.account.api;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Public (/api/v1) contracts. Staff-facing; every caller is an employee. */
public final class ApiModels {
    private ApiModels() {
    }

    public record CreateApplicationRequest(
            @NotBlank @Size(max = 30) String cifNo,
            @NotBlank @Size(max = 30) String productCode,
            @Size(max = 150) String accountName,
            @DecimalMin("0.0") BigDecimal initialDeposit,
            @Size(max = 3) String currency) {
    }

    public record ApplicationDetail(
            String applicationId,
            String applicationReference,
            String cifNo,
            String productCode,
            String branchCode,
            String currency,
            String accountName,
            BigDecimal requestedInitialDeposit,
            String status,
            String makerEmployeeId,
            String checkerEmployeeId,
            String rejectionReason,
            String createdAccountId,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record DecisionRequest(@Size(max = 500) String remarks) {
    }

    public record RejectionRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record AccountDetail(
            String accountId,
            String accountNumber,
            String maskedAccountNumber,
            String accountName,
            String cifNo,
            String productCode,
            String branchCode,
            String currency,
            String status,
            BigDecimal ledgerBalance,
            BigDecimal heldAmount,
            BigDecimal availableBalance,
            BigDecimal minBalance,
            BigDecimal overdraftLimit,
            BigDecimal interestRate,
            Integer tenureMonths,
            LocalDate maturityDate,
            LocalDate openedOn,
            LocalDate closedOn,
            LocalDate dormantSince,
            long version) {
    }

    /** Ledger, available and held balance in one read, as the endpoint catalogue specifies. */
    public record BalanceView(
            String accountId,
            String currency,
            BigDecimal ledgerBalance,
            BigDecimal heldAmount,
            BigDecimal availableBalance,
            BigDecimal minBalance,
            BigDecimal overdraftLimit,
            Instant asOf) {
    }

    public record BalanceHistoryEntry(
            Long historyId,
            String transactionId,
            String transactionReference,
            String direction,
            BigDecimal amount,
            BigDecimal ledgerBalanceBefore,
            BigDecimal ledgerBalanceAfter,
            LocalDate businessDate,
            Instant createdAt) {
    }

    public record StatusHistoryEntry(
            Long id,
            String fromStatus,
            String toStatus,
            String reason,
            String changedByEmployeeId,
            String source,
            Instant changedAt) {
    }

    public record HolderDetail(
            String holderId,
            String cifNo,
            String holderRole,
            Integer holderSequence,
            String status,
            Instant addedAt) {
    }

    public record AddHolderRequest(
            @NotBlank @Size(max = 30) String cifNo,
            @Pattern(regexp = "PRIMARY|JOINT") String holderRole) {
    }

    public record HoldDetail(
            String holdId,
            String transactionId,
            BigDecimal amount,
            String currency,
            String reason,
            String holdType,
            String status,
            Instant createdAt,
            Instant releasedAt) {
    }

    public record ManualHoldRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Size(max = 64) String reason,
            @Pattern(regexp = "LIEN|MANUAL") String holdType) {
    }

    public record StatusChangeRequest(@Size(max = 500) String reason) {
    }

    public record LimitsView(
            String accountId,
            BigDecimal perTransactionLimit,
            BigDecimal dailyWithdrawalLimit,
            Instant updatedAt) {
    }

    public record LimitsRequest(
            @NotNull @DecimalMin("0.0") BigDecimal perTransactionLimit,
            @NotNull @DecimalMin("0.0") BigDecimal dailyWithdrawalLimit) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }
}
