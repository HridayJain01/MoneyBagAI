package com.harshul.demo.kyc.service;
import com.harshul.demo.kyc.dto.request.CreateKycSessionRequest;
import com.harshul.demo.kyc.dto.request.ManualDecisionRequest;
import com.harshul.demo.kyc.dto.response.*;
import com.harshul.demo.kyc.entity.*;
import org.springframework.cglib.core.Local;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import com.harshul.demo.kyc.engine.VerificationEngine;
import com.harshul.demo.kyc.storage.FileStorageService;
import org.springframework.stereotype.Service;
import java.time.Instant;


@Service
public class KycServiceImpl implements KycService {

    private final FileStorageService storageService;
    private final VerificationEngine verificationEngine;


    public KycServiceImpl(
            FileStorageService storageService,
            VerificationEngine verificationEngine
    ) {
        this.storageService = storageService;
        this.verificationEngine = verificationEngine;
    }

    @Override
    public KycSessionResponse createSession(CreateKycSessionRequest request) {
        KycSessionEntity session = new KycSessionEntity();
        session.setExternalUserId(request.externalUserId());
        session.setPurpose(request.purpose());
        session.setStatus(KycSessionStatus.CREATED);
        session.setDocumentType(request.documentType());
        KycSessionEntity entity = storageService.createSession(session);

        LocalDateTime now = entity.getCreatedAt();
        String sessionId = entity.getId();

        return new KycSessionResponse(
                sessionId,
                request.externalUserId(),
                request.purpose(),
                request.documentType(),
                entity.getStatus(),
                now,
                now
        );
    }

    @Override
    public KycSessionResponse getSession(String sessionId) {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);

        KycSessionEntity session = storageService.getSession(target);
        KycSessionResponse response = new KycSessionResponse();
        response.setSessionId(sessionId);
        response.setStatus(session.getStatus());
        response.setCreatedAt(session.getCreatedAt());
        response.setExternalUserId(session.getExternalUserId());
        response.setUpdatedAt(session.getUpdatedAt());
        response.setPurpose(session.getPurpose());
        return response;
    }

    @Override
    public KycDocumentResponse uploadDocument(
            String sessionId,
            DocumentType documentType,
            MultipartFile file
    ) throws IOException {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);

        KycSessionEntity session = storageService.getSession(target);
        DocumentType type = documentType;
        KycDocumentEntity document = storageService.storeDocument(sessionId, type, file);


        KycDocumentResponse response = new KycDocumentResponse(
                sessionId,
                document.getId(),
                documentType,
                document.getOriginalFileName()
        );

        session.setStatus(KycSessionStatus.DOCUMENT_UPLOADED);
        storageService.updateSession(session);

        return response;
    }

    @Override
    public List<KycSessionResponse> findPendingSessions(String externalUserId) {
        return storageService.findPendingSessions(externalUserId).stream().map(session -> new KycSessionResponse(
                session.getId(), session.getExternalUserId(), session.getPurpose(), session.getDocumentType(),
                session.getStatus(), session.getCreatedAt(), session.getUpdatedAt())).toList();
    }
    @Override
    public List<KycSessionResponse> findSessions(String externalUserId, DocumentType documentType) {
        return storageService.findSessions(externalUserId, documentType).stream().map(session -> new KycSessionResponse(
                session.getId(), session.getExternalUserId(), session.getPurpose(), session.getDocumentType(),
                session.getStatus(), session.getCreatedAt(), session.getUpdatedAt())).toList();
    }

    @Override
    public KycDocumentEntity getDocument(String sessionId, DocumentType documentType) throws IOException {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);
        storageService.getSession(target);
        return storageService.getDocument(sessionId, documentType);
    }

    @Override
    public FrameUploadResponse processFrames(String sessionId, List<MultipartFile> frames) throws IOException {

        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);
        KycSessionEntity session = storageService.getSession(target);

        if (frames == null || frames.size() != 5) {
            throw new IllegalArgumentException("Exactly 5 frames must be uploaded.");
        }

        storageService.deleteFrames(sessionId);
        int index = 1;
        for (MultipartFile frame : frames) {
            storageService.storeFrame(sessionId, frame, index++);
        }

        FrameUploadResponse response =
                new FrameUploadResponse(
                        sessionId,
                        frames.size(),
                        "FRAMES_UPLOADED"
                );


        session.setStatus(KycSessionStatus.FRAME_CAPTURED);
        storageService.updateSession(session);
        return response;
    }

    @Override
    public List<CapturedFrameResponse> getFrames(String sessionId) throws IOException {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);
        storageService.getSession(target);

        return storageService.getFrames(sessionId)
                .stream()
                .map(frame -> new CapturedFrameResponse(
                        frame.getId(),
                        sessionId,
                        frame.getFrameNumber(),
                        frame.getOriginalFileName(),
                        frame.getContentType(),
                        frame.getContent() == null ? 0 : frame.getContent().length
                ))
                .toList();
    }

    @Override
    public CapturedFrameEntity getFrame(String sessionId, int frameNumber) throws IOException {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);
        storageService.getSession(target);
        return storageService.getFrame(sessionId, frameNumber);
    }

    @Override
    public KycVerificationResultResponse getResult(String sessionId) {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);

        KycSessionEntity session = storageService.getSession(target);
        KycVerificationEntity result = new KycVerificationEntity();
        result.setSession(session);

        KycVerificationEntity verification = storageService.getResult(result);
        return new KycVerificationResultResponse(
                sessionId,
                verification.getResult(),
                session.getStatus() == KycSessionStatus.VERIFIED
        );
    }


    @Override
    public KycDecisionResponse approve(
            String sessionId,
            ManualDecisionRequest request
    ) {
        getSession(sessionId);

        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);

        KycSessionEntity session = storageService.getSession(target);
        session.setStatus(KycSessionStatus.VERIFIED);
        storageService.updateSession(session);


        KycVerificationEntity result = new KycVerificationEntity();
        result.setSession(session);
        result.setResult(request.remarks());
        result.setReviewerId(request.reviewerId());
        storageService.createResult(result);

        return new KycDecisionResponse(
                sessionId,
                "APPROVED",
                request.reviewerId(),
                Instant.now(),
                request.remarks()
        );
    }

    @Override
    public KycDecisionResponse reject(String sessionId, ManualDecisionRequest request) {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);
        KycSessionEntity session = storageService.getSession(target);


        KycVerificationEntity result = new KycVerificationEntity();
        result.setReviewerId(request.reviewerId());
        result.setResult(request.remarks());
        result.setSession(session);
        storageService.createResult(result);

        session.setStatus(KycSessionStatus.REJECTED);
        storageService.updateSession(session);

        return new KycDecisionResponse(
                sessionId,
                "REJECTED",
                request.reviewerId(),
                Instant.now(),
                request.remarks()
        );
    }

}
