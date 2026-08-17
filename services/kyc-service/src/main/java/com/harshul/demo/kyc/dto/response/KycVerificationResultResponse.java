package com.harshul.demo.kyc.dto.response;

public record KycVerificationResultResponse(
        String sessionId,
        String decision,
        boolean verified
) {}
