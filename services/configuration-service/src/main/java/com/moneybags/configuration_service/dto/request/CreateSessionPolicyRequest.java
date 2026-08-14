package com.moneybags.configuration_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateSessionPolicyRequest {

    @NotNull(message = "idleTimeoutMinutes is required")
    @Min(value = 1, message = "idleTimeoutMinutes must be at least 1")
    private Integer idleTimeoutMinutes;

    @NotNull(message = "absoluteTimeoutMinutes is required")
    @Min(value = 1, message = "absoluteTimeoutMinutes must be at least 1")
    private Integer absoluteTimeoutMinutes;

    @Min(value = 1, message = "maxConcurrentSessions must be at least 1")
    private Integer maxConcurrentSessions = 3;

    private LocalDateTime effectiveFrom;

    public Integer getIdleTimeoutMinutes() { return idleTimeoutMinutes; }
    public void setIdleTimeoutMinutes(Integer idleTimeoutMinutes) { this.idleTimeoutMinutes = idleTimeoutMinutes; }
    public Integer getAbsoluteTimeoutMinutes() { return absoluteTimeoutMinutes; }
    public void setAbsoluteTimeoutMinutes(Integer absoluteTimeoutMinutes) { this.absoluteTimeoutMinutes = absoluteTimeoutMinutes; }
    public Integer getMaxConcurrentSessions() { return maxConcurrentSessions; }
    public void setMaxConcurrentSessions(Integer maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
