package com.moneybags.branch_employee_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class WorkingHoursDayRequest {

    @NotBlank(message = "dayOfWeek is required")
    @Pattern(regexp = "MON|TUE|WED|THU|FRI|SAT|SUN", message = "dayOfWeek must be one of MON..SUN")
    private String dayOfWeek;

    private String openTime;
    private String closeTime;
    private boolean closed;

    // getters and setters
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
}
