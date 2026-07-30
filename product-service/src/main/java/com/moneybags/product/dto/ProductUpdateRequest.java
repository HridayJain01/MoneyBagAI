package com.moneybags.product.dto;

import com.moneybags.product.enums.ProductStatus;
import com.moneybags.product.enums.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank @Size(max = 100) String productName,
        @NotNull ProductType productType,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("0.0") BigDecimal interestRate,
        @NotNull @DecimalMin("0.0") BigDecimal minBalance,
        @NotNull @DecimalMin("0.0") BigDecimal maxWithdrawalPerDay,
        @NotNull @PositiveOrZero Integer freeTxnPerMonth,
        @NotNull ProductStatus status
) {
}
