package com.moneybags.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /** Stable business key; other services store this, never a surrogate id. */
    @Id
    @Column(name = "product_code", length = 30)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    private ProductType productType;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "min_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal minBalance;

    @Column(name = "min_opening_deposit", nullable = false, precision = 19, scale = 2)
    private BigDecimal minOpeningDeposit;

    @Column(name = "max_withdrawal_per_day", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxWithdrawalPerDay;

    @Column(name = "free_txn_per_month", nullable = false)
    private Integer freeTxnPerMonth;

    /** NULL for demand deposits, required for FD/RD -- enforced by a DB check too. */
    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "allows_overdraft", nullable = false)
    private Boolean allowsOverdraft;

    @Column(name = "requires_funding", nullable = false)
    private Boolean requiresFunding;

    @Column(name = "min_age")
    private Integer minAge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

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

    /** Open to new business on the given business date. */
    public boolean isOpenOn(LocalDate businessDate) {
        return status == ProductStatus.ACTIVE
                && !businessDate.isBefore(effectiveFrom)
                && (effectiveTo == null || !businessDate.isAfter(effectiveTo));
    }
}
