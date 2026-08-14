package com.moneybags.configuration_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_policy")
public class OtpPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "expiry_seconds", nullable = false)
    private Integer expirySeconds;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "resend_cooldown_seconds", nullable = false)
    private Integer resendCooldownSeconds;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getExpirySeconds() { return expirySeconds; }
    public void setExpirySeconds(Integer expirySeconds) { this.expirySeconds = expirySeconds; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    public Integer getResendCooldownSeconds() { return resendCooldownSeconds; }
    public void setResendCooldownSeconds(Integer resendCooldownSeconds) { this.resendCooldownSeconds = resendCooldownSeconds; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
