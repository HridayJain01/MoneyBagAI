package com.moneybags.branch_employee_service.dto.response;

import java.math.BigDecimal;

public class ApprovalAuthorityResponse {

    private final Long employeeId;
    private final String actionType;
    private final BigDecimal maxAmount;
    private final String currency;

    public ApprovalAuthorityResponse(Long employeeId, String actionType, BigDecimal maxAmount, String currency) {
        this.employeeId = employeeId;
        this.actionType = actionType;
        this.maxAmount = maxAmount;
        this.currency = currency;
    }

    public Long getEmployeeId() { return employeeId; }
    public String getActionType() { return actionType; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public String getCurrency() { return currency; }
}
