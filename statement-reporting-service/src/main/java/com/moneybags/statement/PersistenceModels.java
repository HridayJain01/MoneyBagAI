package com.moneybags.statement;

import com.moneybags.statement.ApiModels.*;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="account_read_models") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class AccountReadModel {
    @Id @Column(name="account_id",length=64) String accountId;
    @Column(name="customer_id",nullable=false,length=64) String customerId;
    @Column(name="branch_id",length=64) String branchId;
    @Column(name="masked_account_number",length=32) String maskedAccountNumber;
    @Column(name="account_name",length=160) String accountName;
    @Column(nullable=false,length=24) String status;
    @Column(nullable=false,length=3,columnDefinition="char(3)") String currency;
    @Column(name="current_balance",nullable=false,precision=19,scale=2) BigDecimal currentBalance;
    @Column(name="dormant_since") LocalDate dormantSince;
    @Column(name="source_updated_at",nullable=false) Instant sourceUpdatedAt;
}

@Data @NoArgsConstructor @AllArgsConstructor
class TransactionReadId implements Serializable { String transactionId; String accountId; Direction direction; }

@Entity @Table(name="transaction_read_models") @IdClass(TransactionReadId.class) @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class TransactionReadModel {
    @Id @Column(name="transaction_id",length=64) String transactionId;
    @Id @Column(name="account_id",length=64) String accountId;
    @Id @Enumerated(EnumType.STRING) @Column(length=8) Direction direction;
    @Column(name="ledger_entry_id",length=64) String ledgerEntryId;
    @Column(name="transaction_reference",nullable=false,length=64) String transactionReference;
    @Column(name="customer_id",nullable=false,length=64) String customerId;
    @Column(name="branch_id",length=64) String branchId;
    @Column(nullable=false,precision=19,scale=2) BigDecimal amount;
    @Column(name="fee_amount",nullable=false,precision=19,scale=2) BigDecimal feeAmount;
    @Column(nullable=false,length=3,columnDefinition="char(3)") String currency;
    @Column(name="transaction_type",nullable=false,length=32) String transactionType;
    @Column(nullable=false,length=32) String status;
    @Column(length=500) String narration;
    @Column(name="reversal_of_transaction_id",length=64) String reversalOfTransactionId;
    @Column(name="posted_at",nullable=false) Instant postedAt;
    @Column(name="balance_after",precision=19,scale=2) BigDecimal balanceAfter;
    @Column(name="source_updated_at",nullable=false) Instant sourceUpdatedAt;
}

@Entity @Table(name="consumed_source_events") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class ConsumedSourceEvent {
    @Id @Column(name="source_event_id",length=128) String sourceEventId;
    @Column(name="source_type",nullable=false,length=32) String sourceType;
    @Column(name="request_hash",nullable=false,length=64,columnDefinition="char(64)") String requestHash;
    @Column(name="consumed_at",nullable=false) Instant consumedAt;
}

@Entity @Table(name="statement_requests") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class StatementRequestEntity {
    @Id @Column(name="statement_request_id",length=36) String id;
    @Column(name="request_ref",nullable=false,unique=true,length=128) String requestRef;
    @Column(name="request_hash",nullable=false,length=64,columnDefinition="char(64)") String requestHash;
    @Column(name="account_id",nullable=false,length=64) String accountId;
    @Column(name="requested_by_user_id",nullable=false,length=64) String requestedByUserId;
    @Column(name="requested_by_cif",length=64) String requestedByCif;
    @Column(name="requester_branch_id",length=64) String requesterBranchId;
    @Column(name="from_date",nullable=false) LocalDate fromDate;
    @Column(name="to_date",nullable=false) LocalDate toDate;
    @Enumerated(EnumType.STRING) @Column(name="output_format",nullable=false,length=8) OutputFormat outputFormat;
    @Enumerated(EnumType.STRING) @Column(name="statement_kind",nullable=false,length=16) StatementKind statementKind;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) RequestStatus status;
    @Column(name="source_snapshot_at") Instant sourceSnapshotAt;
    @Column(name="safe_error_code",length=64) String safeErrorCode;
    @Column(name="safe_error_message",length=500) String safeErrorMessage;
    @Version @Column(name="version_no") long version;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();Instant n=Instant.now();if(createdAt==null)createdAt=n;updatedAt=n;}
    @PreUpdate void update(){updatedAt=Instant.now();}
}

@Entity @Table(name="generated_statement_files") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class GeneratedFileEntity {
    @Id @Column(name="file_id",length=36) String id;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="statement_request_id") StatementRequestEntity request;
    @Column(name="storage_key",nullable=false,unique=true,length=255) String storageKey;
    @Column(name="content_type",nullable=false,length=100) String contentType;
    @Column(name="file_name",nullable=false,length=255) String fileName;
    @Column(name="file_size_bytes",nullable=false) long fileSizeBytes;
    @Column(name="checksum_sha256",nullable=false,length=64,columnDefinition="char(64)") String checksumSha256;
    @Column(name="expires_at",nullable=false) Instant expiresAt;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();if(createdAt==null)createdAt=Instant.now();}
}

@Entity @Table(name="statement_download_history") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class DownloadHistoryEntity {
    @Id @Column(name="download_id",length=36) String id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="statement_request_id") StatementRequestEntity request;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="file_id") GeneratedFileEntity file;
    @Column(name="downloaded_by_user_id",nullable=false,length=64) String downloadedByUserId;
    @Column(name="source_ip",length=64) String sourceIp;
    @Column(nullable=false,length=16) String outcome;
    @Column(name="reason_code",length=64) String reasonCode;
    @Column(name="downloaded_at",nullable=false) Instant downloadedAt;
    @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();if(downloadedAt==null)downloadedAt=Instant.now();}
}

@Entity @Table(name="report_schedules") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
class ReportScheduleEntity {
    @Id @Column(name="schedule_id",length=36) String id;
    @Column(name="owner_user_id",nullable=false,length=64) String ownerUserId;
    @Column(name="owner_cif",length=64) String ownerCif;
    @Column(name="branch_id",length=64) String branchId;
    @Column(name="account_id",length=64) String accountId;
    @Column(name="report_type",nullable=false,length=32) String reportType;
    @Enumerated(EnumType.STRING) @Column(name="output_format",nullable=false,length=8) OutputFormat outputFormat;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) Frequency frequency;
    @Column(name="next_run_at",nullable=false) Instant nextRunAt;
    @Column(nullable=false) boolean active;
    @Column(name="last_run_at") Instant lastRunAt;
    @Version @Column(name="version_no") long version;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @PrePersist void create(){if(id==null)id=UUID.randomUUID().toString();Instant n=Instant.now();if(createdAt==null)createdAt=n;updatedAt=n;}
    @PreUpdate void update(){updatedAt=Instant.now();}
}
