package com.moneybags.product.dto;

import java.math.BigDecimal;

public record ChargeDetail(Long chargeId, String chargeType, BigDecimal amount, String frequency) {
}
