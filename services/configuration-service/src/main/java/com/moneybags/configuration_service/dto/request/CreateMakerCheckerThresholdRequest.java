package com.moneybags.configuration_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateMakerCheckerThresholdRequest {

    @NotBlank(message = "actionType is required")
    private String actionType;

    @NotNull(message = "thresholdAmount is required")
    @Positive(message = "thresholdAmount must be positive")
    private BigDecimal thresholdAmount;

    @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter uppercase code")
    private String currency;

    private LocalDateTime effectiveFrom;

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
