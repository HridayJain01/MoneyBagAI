package com.harshul.demo.kyc.dto.request;

import com.harshul.demo.kyc.entity.DocumentType;

public record CreateKycSessionRequest(
        String externalUserId,
        String purpose,
        DocumentType documentType
) {}
