package com.moneybags.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RuleRequest(
        @NotBlank @Size(max = 60) String ruleKey,
        @NotBlank @Size(max = 255) String ruleValue,
        @NotBlank @Size(max = 20) String dataType) {
}
