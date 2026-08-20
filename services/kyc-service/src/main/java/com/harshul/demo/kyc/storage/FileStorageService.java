package com.harshul.demo.kyc.storage;
import com.harshul.demo.kyc.entity.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface FileStorageService {
    KycDocumentEntity storeDocument(
            String sessionId,
            DocumentType documentType,
            MultipartFile file
    ) throws IOException;

    CapturedFrameEntity storeFrame(
            String sessionId,
            MultipartFile file,
            int frameNumber
    ) throws IOException;

    List<CapturedFrameEntity> getFrames(
            String sessionId
    ) throws IOException;

    CapturedFrameEntity getFrame(
            String sessionId,
            int frameNumber
    ) throws IOException;

    KycDocumentEntity getDocument(
            String sessionId,
            DocumentType documentType
    ) throws IOException;

    void deleteResult(KycVerificationEntity result);
    KycVerificationEntity createResult(KycVerificationEntity result);
    KycVerificationEntity getResult(KycVerificationEntity result);
    KycVerificationEntity updateResult(KycVerificationEntity result);


    void deleteSession(String sessionId) throws IOException;
    KycSessionEntity createSession(KycSessionEntity session);
    KycSessionEntity getSession(KycSessionEntity session);
    List<KycSessionEntity> findSessions(String cifNo);
    List<KycSessionEntity> findPendingSessions(String cifNo);
    List<KycSessionEntity> findSessions(String cifNo);
    KycSessionEntity updateSession(KycSessionEntity session);
}
