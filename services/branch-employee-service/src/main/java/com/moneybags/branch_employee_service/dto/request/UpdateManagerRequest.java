package com.moneybags.branch_employee_service.dto.request;

import jakarta.validation.constraints.Positive;

public class UpdateManagerRequest {

    @Positive(message = "reportingManagerId must be positive")
    private Long reportingManagerId;

    public Long getReportingManagerId() { return reportingManagerId; }
    public void setReportingManagerId(Long reportingManagerId) { this.reportingManagerId = reportingManagerId; }
}
