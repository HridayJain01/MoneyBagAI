package com.moneybags.configuration_service.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    @Column(name = "flag_key")
    private String flagKey;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "enabled", nullable = false, length = 1)
    private String enabled;

    @Column(name = "description")
    private String description;

    @Lob
    @Column(name = "targeting_rule", columnDefinition = "TEXT")
    private String targetingRule;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // getters and setters
    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTargetingRule() { return targetingRule; }
    public void setTargetingRule(String targetingRule) { this.targetingRule = targetingRule; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
