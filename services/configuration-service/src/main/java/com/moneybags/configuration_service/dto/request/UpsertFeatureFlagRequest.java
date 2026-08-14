package com.moneybags.configuration_service.dto.request;


import jakarta.validation.constraints.NotNull;

public class UpsertFeatureFlagRequest {

    @NotNull(message = "enabled is required")
    private Boolean enabled;

    private String description;
    private String targetingRule;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTargetingRule() { return targetingRule; }
    public void setTargetingRule(String targetingRule) { this.targetingRule = targetingRule; }
}
