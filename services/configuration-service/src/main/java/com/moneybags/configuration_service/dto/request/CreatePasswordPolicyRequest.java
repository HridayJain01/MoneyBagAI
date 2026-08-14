package com.moneybags.configuration_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreatePasswordPolicyRequest {

    @NotNull(message = "minLength is required")
    @Min(value = 4, message = "minLength must be at least 4")
    private Integer minLength;

    private Boolean requireUpper = true;
    private Boolean requireDigit = true;
    private Boolean requireSpecial = true;

    @Min(value = 0, message = "historyCount must be at least 0")
    private Integer historyCount = 5;

    @Min(value = 1, message = "maxAgeDays must be at least 1")
    private Integer maxAgeDays = 90;

    private LocalDateTime effectiveFrom;

    public Integer getMinLength() { return minLength; }
    public void setMinLength(Integer minLength) { this.minLength = minLength; }
    public Boolean getRequireUpper() { return requireUpper; }
    public void setRequireUpper(Boolean requireUpper) { this.requireUpper = requireUpper; }
    public Boolean getRequireDigit() { return requireDigit; }
    public void setRequireDigit(Boolean requireDigit) { this.requireDigit = requireDigit; }
    public Boolean getRequireSpecial() { return requireSpecial; }
    public void setRequireSpecial(Boolean requireSpecial) { this.requireSpecial = requireSpecial; }
    public Integer getHistoryCount() { return historyCount; }
    public void setHistoryCount(Integer historyCount) { this.historyCount = historyCount; }
    public Integer getMaxAgeDays() { return maxAgeDays; }
    public void setMaxAgeDays(Integer maxAgeDays) { this.maxAgeDays = maxAgeDays; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
