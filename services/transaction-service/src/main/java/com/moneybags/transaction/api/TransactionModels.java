package com.moneybags.transaction.api;

import com.moneybags.transaction.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class TransactionModels {
    private TransactionModels() {}

    public record CreateRequest(
            String sourceAccountId,
            String destinationAccountId,
            String cardId,
            @NotNull @DecimalMin(value="0.0001") BigDecimal amount,
            @DecimalMin(value="0.0000") BigDecimal feeAmount,
            @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
            @NotNull PaymentChannel paymentChannel,
            @NotNull PaymentMethod paymentMethod,
            String upiAddress,
            String chequeNumber,
            String narration,
            String clientReference) {}

    public record ActionRequest(String reason) {}
    public record CallbackRequest(@NotBlank String providerEventId,String externalReference,LocalDate settlementDate,String outcome,String reason) {}
    public record OpeningDepositRequest(
            @NotBlank String accountId,
            @NotNull @DecimalMin(value="0.0001") BigDecimal amount,
            @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
            @NotBlank String applicationReference,
            @NotBlank String initiatedByEmployeeId,
            @NotBlank String branchCode,
            String correlationId) {}
    public record ReversalRequest(@NotBlank String reason) {}
    public record AssignRequest(@NotBlank String investigatorId) {}
    public record ResolveRequest(@NotBlank String resolution,@NotBlank String notes) {}

    public record StatusView(String transactionId,String transactionReference,TransactionStatus status,Instant updatedAt) {}
    public record LegView(int sequenceNo,String role,String direction,String accountId,BigDecimal amount,String currency,String description) {}
    public record HoldView(String holdId,String externalHoldId,String accountId,BigDecimal amount,String currency,String status) {}
    public record JournalLineView(int lineNo,String ledgerAccountCode,String accountId,BigDecimal debit,BigDecimal credit,String description) {}
    public record JournalView(String journalReference,String type,String status,BigDecimal totalDebit,BigDecimal totalCredit,List<JournalLineView> lines) {}
    public record ClearingView(String instructionId,PaymentRail rail,String status,String externalReference,LocalDate settlementDate,String failureReason) {}
    public record HistoryView(String fromStatus,String toStatus,String actorId,String actorSource,String reason,Instant occurredAt) {}
    public record RailDetailsView(String upiAddress,String chequeNumber,String cardId,String clientReference) {}
    public record TransactionView(String transactionId,String transactionReference,TransactionType type,PaymentRail rail,PaymentChannel channel,
            PaymentMethod method,String sourceAccountId,String destinationAccountId,String accountHolderId,BigDecimal amount,BigDecimal feeAmount,
            String currency,TransactionStatus status,String makerEmployeeId,String checkerEmployeeId,String branchCode,String narration,String reversalOfTransactionId,
            String correlationId,Instant createdAt,Instant updatedAt,Instant completedAt,List<LegView> legs,HoldView hold,List<JournalView> journals,
            ClearingView clearing,RailDetailsView railDetails,List<HistoryView> history) {}
    public record LimitQuote(TransactionType transactionType,PaymentRail rail,PaymentChannel channel,String currency,BigDecimal requestedAmount,
            BigDecimal minAmount,BigDecimal maxAmount,BigDecimal dailyLimit,BigDecimal approvalThreshold,boolean allowed,boolean approvalRequired,String reason) {}
}
