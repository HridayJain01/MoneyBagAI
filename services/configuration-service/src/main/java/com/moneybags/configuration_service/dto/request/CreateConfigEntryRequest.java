package com.moneybags.configuration_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateConfigEntryRequest {

    @NotBlank(message = "namespace is required")
    private String namespace;

    @NotBlank(message = "configKey is required")
    private String configKey;

    @NotBlank(message = "configValue is required")
    private String configValue;

    @NotBlank(message = "valueType is required")
    @Pattern(regexp = "STRING|NUMBER|BOOLEAN|JSON", message = "valueType must be STRING, NUMBER, BOOLEAN, or JSON")
    private String valueType;

    private Long updatedBy;

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
