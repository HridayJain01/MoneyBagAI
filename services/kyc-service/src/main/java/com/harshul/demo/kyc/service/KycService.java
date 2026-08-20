package com.harshul.demo.kyc.service;
import com.harshul.demo.kyc.dto.request.CreateKycSessionRequest;
import com.harshul.demo.kyc.dto.request.ManualDecisionRequest;
import com.harshul.demo.kyc.dto.response.*;
import com.harshul.demo.kyc.entity.DocumentType;
import com.harshul.demo.kyc.entity.CapturedFrameEntity;
import com.harshul.demo.kyc.entity.KycDocumentEntity;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.io.IOException;

public interface KycService {

    KycSessionResponse createSession(CreateKycSessionRequest request);

    KycSessionResponse getSession(String sessionId);
    List<KycSessionResponse> findSessions(String cifNo);
    List<KycSessionResponse> findPendingSessions(String cifNo);
    KycSessionResponse submitForReview(String sessionId) throws IOException;

    KycDocumentResponse uploadDocument(
            String sessionId,
            DocumentType documentType,
            MultipartFile file
    ) throws IOException;

    KycDocumentEntity getDocument(
            String sessionId,
            DocumentType documentType
    ) throws IOException;

    FrameUploadResponse processFrames(
            String sessionId,
            List<MultipartFile> frames
    ) throws IOException;

    List<CapturedFrameResponse> getFrames(String sessionId) throws IOException;

    CapturedFrameEntity getFrame(
            String sessionId,
            int frameNumber
    ) throws IOException;

    KycVerificationResultResponse getResult(String sessionId);

    KycDecisionResponse approve(
            String sessionId,
            String reviewerId,
            ManualDecisionRequest request
    );

    KycDecisionResponse reject(
            String sessionId,
            String reviewerId,
            ManualDecisionRequest request
    );
}
