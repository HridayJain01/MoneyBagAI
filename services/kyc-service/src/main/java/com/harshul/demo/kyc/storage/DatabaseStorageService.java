package com.harshul.demo.kyc.storage;

import com.harshul.demo.kyc.entity.*;
import com.harshul.demo.kyc.repository.CapturedFrameRepository;
import com.harshul.demo.kyc.repository.KycDocumentRepository;
import com.harshul.demo.kyc.repository.KycSessionRepository;
import com.harshul.demo.kyc.repository.KycVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class DatabaseStorageService implements FileStorageService{
    @Autowired
    KycSessionRepository sessionRepository;
    @Autowired
    KycDocumentRepository documentRepository;
    @Autowired
    KycVerificationRepository verificationRepository;
    @Autowired
    CapturedFrameRepository capturedFrameRepository;

    @Override
    public KycDocumentEntity storeDocument(String sessionId, DocumentType documentType, MultipartFile file) throws IOException {
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        KycDocumentEntity document = new KycDocumentEntity();
        document.setDocumentType(documentType);
        document.setSession(session);
        document.setOriginalFileName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setContent(file.getBytes());
        document.setSize(file.getSize());

        return documentRepository.save(document);
    }
    @Override
    public CapturedFrameEntity storeFrame(String sessionId, MultipartFile file, int frameNumber) throws IOException {
        var session = sessionRepository.findById(sessionId).orElseThrow(()->new RuntimeException("error at store frame"));
        var frame = new CapturedFrameEntity();
        frame.setFrameNumber(frameNumber);
        frame.setSession(session);
        frame.setOriginalFileName(file.getOriginalFilename());
        frame.setContentType(file.getContentType());
        frame.setContent(file.getBytes());
        return capturedFrameRepository.save(frame);
    }

    @Override
    public List<CapturedFrameEntity> getFrames(String sessionId) throws IOException {
        List<CapturedFrameEntity> frames = capturedFrameRepository.findBySessionIdOrderByFrameNumberAsc(sessionId);
        return frames;
    }

    @Override
    public CapturedFrameEntity getFrame(String sessionId, int frameNumber) throws IOException {
        return capturedFrameRepository.findBySessionIdAndFrameNumber(sessionId, frameNumber)
                .orElseThrow(() -> new RuntimeException("frame not found for this session"));
    }

    @Override
    public KycDocumentEntity getDocument(String sessionId, DocumentType documentType) throws IOException {
        KycDocumentEntity document = documentRepository.findBySessionIdAndDocumentType(sessionId, documentType)
                .orElseThrow(
                        ()->new RuntimeException("session id and document are not consistent")
                );
        return document;
    }

    @Override
    public void deleteResult(KycVerificationEntity result) {}

    @Override
    public KycVerificationEntity createResult(KycVerificationEntity result) {
        String sessionId = result.getSession().getId();
        verificationRepository.findBySessionId(sessionId).ifPresent(existing -> result.setId(existing.getId()));
        return verificationRepository.save(result);
    }

    @Override
    public KycVerificationEntity getResult(KycVerificationEntity result) {
        String sessionId = result.getSession().getId();
        return verificationRepository
                .findBySessionId(sessionId)
                .orElseThrow(()->new RuntimeException("unable to fetch verification result for this session"));
    }

    @Override
    public KycVerificationEntity updateResult(KycVerificationEntity result) {return verificationRepository.save(result);}


    @Override
    public void deleteSession(String sessionId) throws IOException {}

    @Override public KycSessionEntity createSession(KycSessionEntity session)
    { return  sessionRepository.save(session);}

    @Override public KycSessionEntity getSession(KycSessionEntity sesssion)
    {return sessionRepository.findById(sesssion.getId()).orElseThrow(()->new RuntimeException("session with this id not exist"));}

    @Override public List<KycSessionEntity> findPendingSessions(String cifNo) {
        return sessionRepository.findByCifNoAndStatusInOrderByCreatedAtDesc(
                cifNo,
                List.of(KycSessionStatus.CREATED, KycSessionStatus.DOCUMENT_UPLOADED,
                        KycSessionStatus.FRAME_CAPTURED, KycSessionStatus.VERIFICATION_IN_PROGRESS));
    }

    @Override public List<KycSessionEntity> findSessions(String cifNo) {
        return sessionRepository.findByCifNoOrderByCreatedAtDesc(cifNo);
    }

    @Override public KycSessionEntity updateSession(KycSessionEntity session)
    {return sessionRepository.save(session);}

}
