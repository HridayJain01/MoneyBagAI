package com.moneybags.branch_employee_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ApprovalAuthorityItemRequest {

    @NotBlank(message = "actionType is required")
    private String actionType;

    @NotNull(message = "maxAmount is required")
    @Positive(message = "maxAmount must be positive")
    private BigDecimal maxAmount;

    @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter uppercase code")
    private String currency;

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
