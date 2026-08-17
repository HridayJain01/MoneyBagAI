package com.moneybags.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public final class ExternalKycModels {
    private ExternalKycModels() {
    }

    public record KycContext(String cifNo, String customerStatus, String kycStatus) {
    }

    public record KycDecisionRequest(
            @NotBlank String sessionId,
            @NotBlank @Pattern(regexp = "VERIFIED|REJECTED") String status,
            @NotBlank String reviewerId,
            @NotBlank String reason,
            String remarks,
            Instant decidedAt) {
    }

    public record KycDecisionResult(
            String cifNo,
            String sessionId,
            String kycStatus,
            boolean applied) {
    }
}
