package com.moneybags.product.dto;

import com.moneybags.product.enums.ChargeFrequency;

import java.math.BigDecimal;

public record ProductChargeResponse(
        Long chargeId,
        String productCode,
        String chargeType,
        BigDecimal amount,
        ChargeFrequency frequency
) {
}
