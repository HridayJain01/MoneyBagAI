package com.harshul.demo.kyc.dto.response;

import com.harshul.demo.kyc.entity.DocumentType;
import com.harshul.demo.kyc.entity.KycSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KycSessionResponse{
    String sessionId;
    String cifNo;
    String purpose;
    DocumentType documentType;
    KycSessionStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
