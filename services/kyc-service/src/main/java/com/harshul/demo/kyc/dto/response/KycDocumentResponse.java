package com.harshul.demo.kyc.dto.response;

import com.harshul.demo.kyc.entity.DocumentType;

public record KycDocumentResponse(
        String sessionId,
        String documentId,
        DocumentType documentType,
        String fileName
) {}