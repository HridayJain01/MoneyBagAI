package com.moneybags.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProductRequest(
        @Size(max = 120) String productName,
        @Size(max = 500) String description,
        @DecimalMin("0.0") BigDecimal interestRate,
        @DecimalMin("0.0") BigDecimal minBalance,
        @DecimalMin("0.0") BigDecimal minOpeningDeposit,
        @DecimalMin("0.0") BigDecimal maxWithdrawalPerDay,
        @Min(0) Integer freeTxnPerMonth,
        @Min(0) Integer minAge,
        LocalDate effectiveFrom,
        LocalDate effectiveTo) {
}
