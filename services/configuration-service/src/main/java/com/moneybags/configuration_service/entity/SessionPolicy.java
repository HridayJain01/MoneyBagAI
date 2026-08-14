package com.moneybags.configuration_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_policy")
public class SessionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "idle_timeout_minutes", nullable = false)
    private Integer idleTimeoutMinutes;

    @Column(name = "absolute_timeout_minutes", nullable = false)
    private Integer absoluteTimeoutMinutes;

    @Column(name = "max_concurrent_sessions")
    private Integer maxConcurrentSessions;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getIdleTimeoutMinutes() { return idleTimeoutMinutes; }
    public void setIdleTimeoutMinutes(Integer idleTimeoutMinutes) { this.idleTimeoutMinutes = idleTimeoutMinutes; }
    public Integer getAbsoluteTimeoutMinutes() { return absoluteTimeoutMinutes; }
    public void setAbsoluteTimeoutMinutes(Integer absoluteTimeoutMinutes) { this.absoluteTimeoutMinutes = absoluteTimeoutMinutes; }
    public Integer getMaxConcurrentSessions() { return maxConcurrentSessions; }
    public void setMaxConcurrentSessions(Integer maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
