package com.harshul.demo.kyc.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ManualDecisionRequest(
        @NotBlank String reason,
        String remarks
) {}
