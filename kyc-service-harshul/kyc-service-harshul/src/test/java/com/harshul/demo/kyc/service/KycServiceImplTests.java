package com.harshul.demo.kyc.service;

import com.harshul.demo.kyc.dto.request.CreateKycSessionRequest;
import com.harshul.demo.kyc.dto.request.ManualDecisionRequest;
import com.harshul.demo.kyc.dto.response.CapturedFrameResponse;
import com.harshul.demo.kyc.dto.response.KycSessionResponse;
import com.harshul.demo.kyc.dto.response.KycVerificationResultResponse;
import com.harshul.demo.kyc.entity.CapturedFrameEntity;
import com.harshul.demo.kyc.entity.DocumentType;
import com.harshul.demo.kyc.entity.KycDocumentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KycServiceImplTests {

    @Autowired
    private KycService kycService;

    @Test
    void manualDecisionCanBeReadAndUpdated() {
        KycSessionResponse session = kycService.createSession(
                new CreateKycSessionRequest("user-123", "account-opening", DocumentType.AADHAAR)
        );

        kycService.approve(
                session.getSessionId(),
                new ManualDecisionRequest("reviewer-1", "documents-ok", "Approved manually")
        );

        KycVerificationResultResponse approvedResult = kycService.getResult(session.getSessionId());
        assertThat(approvedResult.decision()).isEqualTo("Approved manually");
        assertThat(approvedResult.verified()).isTrue();

        kycService.reject(
                session.getSessionId(),
                new ManualDecisionRequest("reviewer-2", "bad-match", "Rejected manually")
        );

        KycVerificationResultResponse rejectedResult = kycService.getResult(session.getSessionId());
        assertThat(rejectedResult.decision()).isEqualTo("Rejected manually");
        assertThat(rejectedResult.verified()).isFalse();
    }

    @Test
    void uploadedDocumentAndFramesCanBeFetched() throws Exception {
        KycSessionResponse session = kycService.createSession(
                new CreateKycSessionRequest("user-456", "account-opening", DocumentType.PAN)
        );

        MockMultipartFile document = new MockMultipartFile(
                "file",
                "pan.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );
        kycService.uploadDocument(session.getSessionId(), DocumentType.PAN, document);

        KycDocumentEntity storedDocument = kycService.getDocument(session.getSessionId(), DocumentType.PAN);
        assertThat(storedDocument.getOriginalFileName()).isEqualTo("pan.pdf");
        assertThat(storedDocument.getContent()).isEqualTo("pdf-content".getBytes());

        MockMultipartFile frameOne = new MockMultipartFile(
                "frames",
                "frame-1.jpg",
                "image/jpeg",
                "frame-one".getBytes()
        );
        MockMultipartFile frameTwo = new MockMultipartFile(
                "frames",
                "frame-2.jpg",
                "image/jpeg",
                "frame-two".getBytes()
        );
        MockMultipartFile frameThree = new MockMultipartFile("frames", "frame-3.jpg", "image/jpeg", "frame-three".getBytes());
        MockMultipartFile frameFour = new MockMultipartFile("frames", "frame-4.jpg", "image/jpeg", "frame-four".getBytes());
        MockMultipartFile frameFive = new MockMultipartFile("frames", "frame-5.jpg", "image/jpeg", "frame-five".getBytes());
        kycService.processFrames(session.getSessionId(), List.of(frameOne, frameTwo, frameThree, frameFour, frameFive));

        List<CapturedFrameResponse> frames = kycService.getFrames(session.getSessionId());
        assertThat(frames)
                .extracting(CapturedFrameResponse::frameNumber)
                .containsExactly(1, 2, 3, 4, 5);

        CapturedFrameEntity storedFrame = kycService.getFrame(session.getSessionId(), 2);
        assertThat(storedFrame.getOriginalFileName()).isEqualTo("frame-2.jpg");
        assertThat(storedFrame.getContent()).isEqualTo("frame-two".getBytes());
    }
}
