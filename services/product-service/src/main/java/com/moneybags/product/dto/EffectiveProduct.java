package com.moneybags.product.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EffectiveProduct(
        String productCode,
        String productName,
        String productType,
        String currency,
        BigDecimal interestRate,
        BigDecimal minBalance,
        BigDecimal minOpeningDeposit,
        BigDecimal maxWithdrawalPerDay,
        Integer freeTxnPerMonth,
        Integer tenureMonths,
        BigDecimal overdraftLimit,
        boolean allowsOverdraft,
        boolean requiresFunding,
        Integer minAge,
        String status,
        LocalDate businessDate,
        Long productVersionId,
        Integer versionNumber) {
}
