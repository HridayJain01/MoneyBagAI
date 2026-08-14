package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountApplication {

    @Id
    @Column(name = "application_id", length = 36)
    private String applicationId;

    @Column(name = "application_reference", nullable = false, length = 40)
    private String applicationReference;

    @Column(name = "cif_no", nullable = false, length = 30)
    private String cifNo;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "requested_initial_deposit", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedInitialDeposit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ApplicationStatus status;

    @Column(name = "maker_employee_id", nullable = false, length = 64)
    private String makerEmployeeId;

    /** Must differ from the maker. Enforced in AccountApplicationService. */
    @Column(name = "checker_employee_id", length = 64)
    private String checkerEmployeeId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Product terms as resolved at submission, serialised as JSON. */
    @Lob
    @Column(name = "product_snapshot", columnDefinition = "TEXT")
    private String productSnapshot;

    /** UNIQUE: one application can never produce two accounts, even under a retry. */
    @Column(name = "created_account_id", length = 36)
    private String createdAccountId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
