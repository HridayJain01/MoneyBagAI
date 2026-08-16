package com.moneybags.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable snapshot of every term published for a product version.
 *
 * <p>The mutable {@link Product} row remains the latest catalogue projection. Historical
 * reads use this table, so a later edit never rewrites an earlier response.
 */
@Entity
@Table(name = "product_versions", uniqueConstraints =
        @UniqueConstraint(name = "uk_product_version_number",
                columnNames = {"product_code", "version_number"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_version_id")
    private Long productVersionId;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

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

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    void recordTimestamp() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }

    public boolean appliesOn(LocalDate businessDate) {
        return !businessDate.isBefore(effectiveFrom)
                && (effectiveTo == null || !businessDate.isAfter(effectiveTo));
    }
}
