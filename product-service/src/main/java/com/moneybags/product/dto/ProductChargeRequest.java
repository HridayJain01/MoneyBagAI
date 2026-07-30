package com.moneybags.product.dto;

import com.moneybags.product.enums.ChargeFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductChargeRequest(
        @NotBlank @Size(max = 30) String productCode,
        @NotBlank @Size(max = 50) String chargeType,
        @NotNull @DecimalMin("0.0") BigDecimal amount,
        @NotNull ChargeFrequency frequency
) {
}
