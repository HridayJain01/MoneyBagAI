package com.moneybags.account.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OwnedProductView(
        String ownershipId,
        String ownerAccountId,
        String productCode,
        String productName,
        String productType,
        Long productVersionId,
        Integer productVersionNumber,
        String acquisitionType,
        BigDecimal principalAmount,
        String currency,
        BigDecimal interestRate,
        Integer tenureMonths,
        LocalDate acquiredOn,
        LocalDate maturityDate,
        String status,
        String purchaseTransactionId,
        String reversalTransactionId,
        Instant createdAt,
        Instant updatedAt) {
}
