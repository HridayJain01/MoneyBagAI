package com.moneybags.transaction.entity;

import com.moneybags.transaction.domain.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity @Table(name = "transactions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Transaction {
    @Id @Column(name = "transaction_id", length = 36) private String id;
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 64) private String reference;
    @Enumerated(EnumType.STRING) @Column(name = "transaction_type", nullable = false, length = 32) private TransactionType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private PaymentRail rail;
    @Enumerated(EnumType.STRING) @Column(name = "payment_channel", nullable = false, length = 24) private PaymentChannel channel;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", nullable = false, length = 24) private PaymentMethod method;
    @Column(name = "source_account_id", length = 64) private String sourceAccountId;
    @Column(name = "destination_account_id", length = 64) private String destinationAccountId;
    @Column(name = "customer_id", length = 64) private String customerId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4) private BigDecimal feeAmount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private TransactionStatus status;
    @Column(name = "maker_user_id", nullable = false, length = 64) private String makerUserId;
    @Column(name = "checker_user_id", length = 64) private String checkerUserId;
    @Column(name = "branch_code", length = 32) private String branchCode;
    @Column(length = 500) private String narration;
    @Column(name = "approval_required", nullable = false) private boolean approvalRequired;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "rejection_reason", length = 500) private String rejectionReason;
    @JsonIgnore @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reversal_of_transaction_id") private Transaction reversalOf;
    @Column(name = "correlation_id", nullable = false, length = 64) private String correlationId;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "completed_at") private Instant completedAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (feeAmount == null) feeAmount = BigDecimal.ZERO;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    public BigDecimal totalDebit() { return amount.add(feeAmount); }
}
