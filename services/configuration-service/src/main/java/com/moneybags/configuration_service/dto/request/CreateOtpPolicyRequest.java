package com.moneybags.configuration_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateOtpPolicyRequest {

    @NotNull(message = "expirySeconds is required")
    @Min(value = 30, message = "expirySeconds must be at least 30")
    private Integer expirySeconds;

    @NotNull(message = "maxRetries is required")
    @Min(value = 1, message = "maxRetries must be at least 1")
    private Integer maxRetries;

    @NotNull(message = "resendCooldownSeconds is required")
    @Min(value = 0, message = "resendCooldownSeconds must be at least 0")
    private Integer resendCooldownSeconds;

    private LocalDateTime effectiveFrom;

    public Integer getExpirySeconds() { return expirySeconds; }
    public void setExpirySeconds(Integer expirySeconds) { this.expirySeconds = expirySeconds; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    public Integer getResendCooldownSeconds() { return resendCooldownSeconds; }
    public void setResendCooldownSeconds(Integer resendCooldownSeconds) { this.resendCooldownSeconds = resendCooldownSeconds; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
