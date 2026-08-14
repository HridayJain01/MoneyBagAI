package com.moneybags.branch_employee_service.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class UpdateEmployeeRequest {

    private LocalDate dob;
    private String designation;

    @Positive(message = "reportingManagerId must be positive")
    private Long reportingManagerId;

    @Pattern(regexp = "ACTIVE|ON_LEAVE|RESIGNED", message = "status must be ACTIVE, ON_LEAVE, or RESIGNED")
    private String status;

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public Long getReportingManagerId() { return reportingManagerId; }
    public void setReportingManagerId(Long reportingManagerId) { this.reportingManagerId = reportingManagerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
