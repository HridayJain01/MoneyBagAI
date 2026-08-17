package com.harshul.demo.kyc.dto.response;

import java.time.Instant;

public record KycDecisionResponse(
        String sessionId,
        String decision,
        String reviewerId,
        Instant decidedAt,
        String remarks
) {}