package com.moneybags.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OwnedProductProjectionRequest(
        @NotBlank String ownershipId,
        @NotBlank String action,
        @NotBlank String purchaseTransactionId,
        String reversalTransactionId,
        @NotBlank String ownerAccountId,
        @NotBlank String productCode,
        @NotBlank String productName,
        @NotBlank String productType,
        Long productVersionId,
        Integer productVersionNumber,
        @NotNull @DecimalMin("0.0001") BigDecimal principalAmount,
        @NotBlank String currency,
        @NotNull BigDecimal interestRate,
        Integer tenureMonths,
        @NotNull LocalDate acquiredOn,
        LocalDate maturityDate) {
}
