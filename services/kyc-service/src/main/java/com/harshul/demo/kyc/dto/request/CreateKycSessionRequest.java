package com.harshul.demo.kyc.dto.request;

import com.harshul.demo.kyc.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateKycSessionRequest(
        @NotBlank String cifNo,
        @NotBlank String purpose,
        @NotNull DocumentType documentType
) {}
