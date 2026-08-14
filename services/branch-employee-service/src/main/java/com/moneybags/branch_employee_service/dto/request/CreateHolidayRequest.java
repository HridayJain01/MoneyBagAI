package com.moneybags.branch_employee_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateHolidayRequest {

    @NotNull(message = "holidayDate is required")
    private LocalDate holidayDate;

    private String description;

    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
