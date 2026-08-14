package com.moneybags.configuration_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_policy")
public class PasswordPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "min_length", nullable = false)
    private Integer minLength;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "require_upper", length = 1)
    private String requireUpper;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "require_digit", length = 1)
    private String requireDigit;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "require_special", length = 1)
    private String requireSpecial;

    @Column(name = "history_count")
    private Integer historyCount;

    @Column(name = "max_age_days")
    private Integer maxAgeDays;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getMinLength() { return minLength; }
    public void setMinLength(Integer minLength) { this.minLength = minLength; }
    public String getRequireUpper() { return requireUpper; }
    public void setRequireUpper(String requireUpper) { this.requireUpper = requireUpper; }
    public String getRequireDigit() { return requireDigit; }
    public void setRequireDigit(String requireDigit) { this.requireDigit = requireDigit; }
    public String getRequireSpecial() { return requireSpecial; }
    public void setRequireSpecial(String requireSpecial) { this.requireSpecial = requireSpecial; }
    public Integer getHistoryCount() { return historyCount; }
    public void setHistoryCount(Integer historyCount) { this.historyCount = historyCount; }
    public Integer getMaxAgeDays() { return maxAgeDays; }
    public void setMaxAgeDays(Integer maxAgeDays) { this.maxAgeDays = maxAgeDays; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
