package com.moneybags.statement;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}
    public enum OutputFormat { PDF, CSV, XLSX }
    public enum StatementKind { DATE_RANGE, MONTHLY, YEARLY }
    public enum RequestStatus { PENDING, GENERATING, READY, FAILED, CANCELLED }
    public enum Direction { DEBIT, CREDIT }
    public enum Frequency { DAILY, WEEKLY, MONTHLY, YEARLY }

    public record StatementRequestBody(@NotNull LocalDate fromDate,@NotNull LocalDate toDate,@NotNull OutputFormat outputFormat,StatementKind statementKind) {}
    public record Money(String amount,String currency) { static Money of(BigDecimal v,String c){return new Money(v.setScale(2).toPlainString(),c);} }
    public record EntryView(String transactionId,String ledgerEntryId,String reference,Instant postedAt,String type,Direction direction,
                            String narration,Money amount,Money balanceAfter,String reversalOfTransactionId) {}
    public record MiniStatementView(String accountId,String maskedAccountNumber,String accountName,Money currentBalance,List<EntryView> entries) {}
    public record FileView(String fileId,String fileName,String contentType,long sizeBytes,String checksumSha256,Instant expiresAt) {}
    public record RequestView(String requestId,String requestRef,String accountId,LocalDate fromDate,LocalDate toDate,OutputFormat outputFormat,
                              StatementKind statementKind,RequestStatus status,Instant sourceSnapshotAt,String errorCode,String errorMessage,
                              FileView file,Instant createdAt,Instant updatedAt) {}
    public record DownloadLink(String url,Instant expiresAt) {}
    public record DownloadView(String downloadId,String requestId,String fileId,String userId,String outcome,String reasonCode,Instant downloadedAt) {}
    public record PageView<T>(List<T> items,int page,int size,long totalItems,int totalPages) {}

    public record AccountEvent(@NotBlank String sourceEventId,@NotBlank String accountId,@NotBlank String customerId,String branchId,
                               String maskedAccountNumber,String accountName,@NotBlank String status,@Pattern(regexp="[A-Z]{3}") String currency,
                               @NotNull BigDecimal currentBalance,LocalDate dormantSince,@NotNull Instant sourceUpdatedAt) {}
    public record TransactionEvent(@NotBlank String sourceEventId,@NotBlank String transactionId,String ledgerEntryId,@NotBlank String transactionReference,
                                   @NotBlank String accountId,@NotBlank String customerId,String branchId,@NotNull Direction direction,
                                   @NotNull @DecimalMin("0.01") BigDecimal amount,BigDecimal feeAmount,@Pattern(regexp="[A-Z]{3}") String currency,
                                   @NotBlank String transactionType,@NotBlank String status,String narration,String reversalOfTransactionId,
                                   @NotNull Instant postedAt,BigDecimal balanceAfter,@NotNull Instant sourceUpdatedAt) {}
    public record IngestResult(String sourceEventId,String result) {}

    public record ScheduleBody(String accountId,@NotBlank String reportType,@NotNull OutputFormat outputFormat,@NotNull Frequency frequency,@NotNull Instant nextRunAt) {}
    public record SchedulePatch(OutputFormat outputFormat,Frequency frequency,Instant nextRunAt,Boolean active) {}
    public record ScheduleView(String scheduleId,String ownerUserId,String accountId,String reportType,OutputFormat outputFormat,Frequency frequency,
                               Instant nextRunAt,boolean active,Instant lastRunAt,Instant createdAt,Instant updatedAt) {}
    public record DailyReport(LocalDate date,long transactionCount,Money debitTotal,Money creditTotal,List<EntryView> transactions) {}
    public record DormantAccount(String accountId,String maskedAccountNumber,String customerId,String branchId,LocalDate dormantSince,Money balance) {}
    public record InterestRow(String accountId,String customerId,Money accrued,LocalDate fromDate,LocalDate toDate) {}
    public record ReconciliationReport(LocalDate date,long projectedTransactions,long missingLedgerReferences,long reversalTransactions) {}
    public record CertificateView(String accountId,int fiscalYear,String certificateType,Money grossAmount,Money taxAmount,Instant generatedAt) {}
}
