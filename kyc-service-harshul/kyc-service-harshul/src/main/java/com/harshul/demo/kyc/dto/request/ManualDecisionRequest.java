package com.harshul.demo.kyc.dto.request;

public record ManualDecisionRequest(
        String reviewerId,
        String reason,
        String remarks
) {}