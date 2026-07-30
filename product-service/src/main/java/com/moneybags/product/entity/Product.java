package com.moneybags.product.entity;

import com.moneybags.product.enums.ProductStatus;
import com.moneybags.product.enums.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    private ProductType productType;

    @Column(length = 500)
    private String description;

    @Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "min_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal minBalance;

    @Column(name = "max_withdrawal_per_day", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxWithdrawalPerDay;

    @Column(name = "free_txn_per_month", nullable = false)
    private Integer freeTxnPerMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;
}
