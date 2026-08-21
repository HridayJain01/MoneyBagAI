package com.harshul.demo.kyc.service;
import com.harshul.demo.kyc.client.CustomerKycClient;
import com.harshul.demo.kyc.dto.request.CreateKycSessionRequest;
import com.harshul.demo.kyc.dto.request.ManualDecisionRequest;
import com.harshul.demo.kyc.dto.response.*;
import com.harshul.demo.kyc.entity.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import com.harshul.demo.kyc.engine.VerificationEngine;
import com.harshul.demo.kyc.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;


@Service
@Transactional
public class KycServiceImpl implements KycService {

    private final FileStorageService storageService;
    private final VerificationEngine verificationEngine;
    private final CustomerKycClient customerKycClient;


    public KycServiceImpl(
            FileStorageService storageService,
            VerificationEngine verificationEngine,
            CustomerKycClient customerKycClient
    ) {
        this.storageService = storageService;
        this.verificationEngine = verificationEngine;
        this.customerKycClient = customerKycClient;
    }

    @Override
    public KycSessionResponse createSession(CreateKycSessionRequest request) {
        customerKycClient.context(request.cifNo());
        KycSessionEntity session = new KycSessionEntity();
        session.setCifNo(request.cifNo());
        session.setPurpose(request.purpose());
        session.setStatus(KycSessionStatus.CREATED);
        session.setDocumentType(request.documentType());
        KycSessionEntity entity = storageService.createSession(session);

        LocalDateTime now = entity.getCreatedAt();
        String sessionId = entity.getId();

        return new KycSessionResponse(
                sessionId,
                request.cifNo(),
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
        response.setCifNo(session.getCifNo());
        response.setUpdatedAt(session.getUpdatedAt());
        response.setPurpose(session.getPurpose());
        response.setDocumentType(session.getDocumentType());
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
        if (session.getStatus() != KycSessionStatus.CREATED
                && session.getStatus() != KycSessionStatus.DOCUMENT_UPLOADED) {
            throw new IllegalArgumentException("Evidence cannot be changed after KYC submission.");
        }
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
    public List<KycSessionResponse> findSessions(String cifNo) {
        customerKycClient.context(cifNo);
        return mapSessions(storageService.findSessions(cifNo));
    }

    @Override
    public List<KycSessionResponse> findPendingSessions(String cifNo) {
        customerKycClient.context(cifNo);
        return mapSessions(storageService.findPendingSessions(cifNo));
    }

    private List<KycSessionResponse> mapSessions(List<KycSessionEntity> sessions) {
        return sessions.stream().map(session -> new KycSessionResponse(
                session.getId(), session.getCifNo(), session.getPurpose(), session.getDocumentType(),
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

        if (session.getStatus() != KycSessionStatus.DOCUMENT_UPLOADED
                && session.getStatus() != KycSessionStatus.FRAME_CAPTURED) {
            throw new IllegalArgumentException("Upload the identity proof before capturing frames, and do not change evidence after submission.");
        }

        if (frames == null || frames.size() != 5) {
            throw new IllegalArgumentException("Exactly 5 frames are required.");
        }

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
    public KycSessionResponse submitForReview(String sessionId) throws IOException {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);
        KycSessionEntity session = storageService.getSession(target);

        if (session.getStatus() == KycSessionStatus.VERIFICATION_IN_PROGRESS) {
            return getSession(sessionId);
        }
        if (session.getStatus() != KycSessionStatus.FRAME_CAPTURED) {
            throw new IllegalArgumentException("Capture exactly 5 frames before submitting for review.");
        }

        storageService.getDocument(sessionId, session.getDocumentType());
        if (storageService.getFrames(sessionId).size() != 5) {
            throw new IllegalArgumentException("Exactly 5 frames are required before submission.");
        }

        session.setStatus(KycSessionStatus.VERIFICATION_IN_PROGRESS);
        storageService.updateSession(session);
        return getSession(sessionId);
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
            String reviewerId,
            ManualDecisionRequest request
    ) {
        getSession(sessionId);

        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);

        KycSessionEntity session = storageService.getSession(target);
        requireSubmitted(session);
        session.setStatus(KycSessionStatus.VERIFIED);
        storageService.updateSession(session);


        KycVerificationEntity result = new KycVerificationEntity();
        result.setSession(session);
        result.setResult(decisionDetail(request, "Approved"));
        result.setReviewerId(reviewerId);
        storageService.createResult(result);

        Instant decidedAt = Instant.now();
        synchronizeDecision(session, "VERIFIED", reviewerId, request, decidedAt);

        return new KycDecisionResponse(
                sessionId,
                "APPROVED",
                reviewerId,
                decidedAt,
                request.remarks()
        );
    }

    @Override
    public KycDecisionResponse reject(String sessionId, String reviewerId, ManualDecisionRequest request) {
        KycSessionEntity target = new KycSessionEntity();
        target.setId(sessionId);
        KycSessionEntity session = storageService.getSession(target);
        requireSubmitted(session);


        KycVerificationEntity result = new KycVerificationEntity();
        result.setReviewerId(reviewerId);
        result.setResult(decisionDetail(request, "Rejected"));
        result.setSession(session);
        storageService.createResult(result);

        session.setStatus(KycSessionStatus.REJECTED);
        storageService.updateSession(session);

        Instant decidedAt = Instant.now();
        synchronizeDecision(session, "REJECTED", reviewerId, request, decidedAt);

        return new KycDecisionResponse(
                sessionId,
                "REJECTED",
                reviewerId,
                decidedAt,
                request.remarks()
        );
    }

    private void synchronizeDecision(KycSessionEntity session, String status, String reviewerId,
                                     ManualDecisionRequest request, Instant decidedAt) {
        customerKycClient.synchronizeDecision(session.getCifNo(),
                new CustomerKycClient.KycDecisionSyncRequest(
                        session.getId(), status, reviewerId, request.reason(), request.remarks(), decidedAt));
    }

    private void requireSubmitted(KycSessionEntity session) {
        if (session.getStatus() != KycSessionStatus.VERIFICATION_IN_PROGRESS) {
            throw new IllegalArgumentException("The teller must submit this KYC session before a checker can decide it.");
        }
    }

    private String decisionDetail(ManualDecisionRequest request, String fallback) {
        return request.remarks() == null || request.remarks().isBlank()
                ? fallback + ": " + request.reason()
                : request.remarks().trim();
    }

}
