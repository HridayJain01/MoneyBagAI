package com.moneybags.product.dto;

public record RuleDetail(Long ruleId, String ruleKey, String ruleValue, String dataType, boolean active) {
}
