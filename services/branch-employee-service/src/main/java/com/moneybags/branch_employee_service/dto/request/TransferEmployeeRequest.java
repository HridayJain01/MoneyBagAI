package com.moneybags.branch_employee_service.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class TransferEmployeeRequest {

    @NotNull(message = "toBranchId is required")
    @Positive(message = "toBranchId must be positive")
    private Long toBranchId;

    private String remarks;

    public Long getToBranchId() { return toBranchId; }
    public void setToBranchId(Long toBranchId) { this.toBranchId = toBranchId; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
