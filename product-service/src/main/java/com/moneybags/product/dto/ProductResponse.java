package com.moneybags.product.dto;

import com.moneybags.product.enums.ProductStatus;
import com.moneybags.product.enums.ProductType;

import java.math.BigDecimal;

public record ProductResponse(
        String productCode,
        String productName,
        ProductType productType,
        String description,
        BigDecimal interestRate,
        BigDecimal minBalance,
        BigDecimal maxWithdrawalPerDay,
        Integer freeTxnPerMonth,
        ProductStatus status
) {
}
