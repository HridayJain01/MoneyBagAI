package com.moneybags.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProductDetail(
        String productCode,
        String productName,
        String productType,
        String description,
        String currency,
        BigDecimal interestRate,
        BigDecimal minBalance,
        BigDecimal minOpeningDeposit,
        BigDecimal maxWithdrawalPerDay,
        Integer freeTxnPerMonth,
        Integer tenureMonths,
        boolean allowsOverdraft,
        boolean requiresFunding,
        Integer minAge,
        String status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Instant createdAt,
        List<ChargeDetail> charges,
        List<RuleDetail> rules,
        Long productVersionId,
        Integer versionNumber) {
}
