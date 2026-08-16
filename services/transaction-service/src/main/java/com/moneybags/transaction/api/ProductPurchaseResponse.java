package com.moneybags.transaction.api;

import com.moneybags.transaction.domain.ProductPurchaseStatus;
import com.moneybags.transaction.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProductPurchaseResponse(
        String purchaseId,
        String transactionId,
        String transactionReference,
        TransactionStatus transactionStatus,
        String ownerAccountId,
        String productCode,
        String productName,
        String productType,
        Long productVersionId,
        Integer productVersionNumber,
        BigDecimal principalAmount,
        String currency,
        BigDecimal interestRate,
        Integer tenureMonths,
        LocalDate purchasedOn,
        LocalDate maturityDate,
        ProductPurchaseStatus ownershipStatus,
        String reversalTransactionId,
        Instant createdAt,
        Instant updatedAt) {
}
