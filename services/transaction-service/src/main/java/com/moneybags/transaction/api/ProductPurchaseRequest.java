package com.moneybags.transaction.api;

import com.moneybags.transaction.domain.PaymentChannel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ProductPurchaseRequest(
        @NotBlank String sourceAccountId,
        @NotBlank String productCode,
        @NotNull @DecimalMin("0.0001") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull PaymentChannel paymentChannel,
        String narration,
        String clientReference) {
}
