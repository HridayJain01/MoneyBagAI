package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "account_id", length = 36)
    private String accountId;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "masked_account_number", nullable = false, length = 24)
    private String maskedAccountNumber;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    /** Logical reference to customer-service. Doubles as accountHolderId and customerId. */
    @Column(name = "cif_no", nullable = false, length = 30)
    private String cifNo;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    /** Carried downstream as both X-Branch-Code and X-Branch-Id. */
    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;

    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AccountStatus status;

    /** Moves ONLY through applied projections. Holds never touch it. */
    @Column(name = "ledger_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal ledgerBalance;

    /** Sum of HELD funds_holds. Maintained incrementally, reconciled on a schedule. */
    @Column(name = "held_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal heldAmount;

    // --- Product terms, snapshotted at opening -----------------------------

    @Column(name = "min_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal minBalance;

    @Column(name = "overdraft_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal overdraftLimit;

    @Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    // --- Lifecycle ---------------------------------------------------------

    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;

    @Column(name = "closed_on")
    private LocalDate closedOn;

    @Column(name = "dormant_since")
    private LocalDate dormantSince;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "application_id", length = 36)
    private String applicationId;

    /**
     * Primitive long, not Long: {@code AccountClient.AccountContext} declares
     * {@code long version}, and Jackson cannot bind null to a primitive. A nullable
     * field here would surface as a deserialization failure inside transaction-service.
     */
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

    /**
     * Derived, never stored. Storing it would create a second source of truth that
     * drifts from ledger_balance and held_amount the first time a write is missed.
     */
    public BigDecimal availableBalance() {
        return ledgerBalance
                .subtract(heldAmount)
                .add(overdraftLimit);
    }
}
