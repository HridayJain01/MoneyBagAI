package com.moneybags.product.dto;

import com.moneybags.product.entity.ProductType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductRequest(
        @NotBlank @Size(max = 30) String productCode,
        @NotBlank @Size(max = 120) String productName,
        @NotNull ProductType productType,
        @Size(max = 500) String description,
        @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull @DecimalMin("0.0") BigDecimal interestRate,
        @NotNull @DecimalMin("0.0") BigDecimal minBalance,
        @DecimalMin("0.0") BigDecimal minOpeningDeposit,
        @DecimalMin("0.0") BigDecimal maxWithdrawalPerDay,
        @Min(0) Integer freeTxnPerMonth,
        @Min(1) Integer tenureMonths,
        Boolean allowsOverdraft,
        Boolean requiresFunding,
        @Min(0) Integer minAge,
        LocalDate effectiveFrom) {
}
