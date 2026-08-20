package com.harshul.demo.kyc.service;

import com.harshul.demo.kyc.client.CustomerKycClient;
import com.harshul.demo.kyc.dto.request.CreateKycSessionRequest;
import com.harshul.demo.kyc.dto.request.ManualDecisionRequest;
import com.harshul.demo.kyc.dto.response.CapturedFrameResponse;
import com.harshul.demo.kyc.dto.response.KycSessionResponse;
import com.harshul.demo.kyc.dto.response.KycVerificationResultResponse;
import com.harshul.demo.kyc.entity.CapturedFrameEntity;
import com.harshul.demo.kyc.entity.DocumentType;
import com.harshul.demo.kyc.entity.KycDocumentEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class KycServiceImplTests {

    @Autowired
    private KycService kycService;

    @MockBean
    private CustomerKycClient customerKycClient;

    @BeforeEach
    void customerExists() {
        when(customerKycClient.context(anyString()))
                .thenAnswer(invocation -> new CustomerKycClient.CustomerKycContext(
                        invocation.getArgument(0), "ACTIVE", "PENDING"));
    }

    @Test
    void manualDecisionCanBeReadAndUpdated() {
        KycSessionResponse session = kycService.createSession(
                new CreateKycSessionRequest("CIF900101", "account-opening", DocumentType.AADHAAR)
        );

        kycService.approve(
                session.getSessionId(),
                "reviewer-1",
                new ManualDecisionRequest("documents-ok", "Approved manually")
        );

        KycVerificationResultResponse approvedResult = kycService.getResult(session.getSessionId());
        assertThat(approvedResult.decision()).isEqualTo("Approved manually");
        assertThat(approvedResult.verified()).isTrue();

        kycService.reject(
                session.getSessionId(),
                "reviewer-2",
                new ManualDecisionRequest("bad-match", "Rejected manually")
        );

        KycVerificationResultResponse rejectedResult = kycService.getResult(session.getSessionId());
        assertThat(rejectedResult.decision()).isEqualTo("Rejected manually");
        assertThat(rejectedResult.verified()).isFalse();

        assertThat(kycService.findSessions("CIF900101"))
                .extracting(KycSessionResponse::getSessionId)
                .contains(session.getSessionId());
    }

    @Test
    void uploadedDocumentAndFramesCanBeFetched() throws Exception {
        KycSessionResponse session = kycService.createSession(
                new CreateKycSessionRequest("CIF900102", "account-opening", DocumentType.PAN)
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
        kycService.processFrames(session.getSessionId(), List.of(frameOne, frameTwo));

        List<CapturedFrameResponse> frames = kycService.getFrames(session.getSessionId());
        assertThat(frames)
                .extracting(CapturedFrameResponse::frameNumber)
                .containsExactly(1, 2);

        CapturedFrameEntity storedFrame = kycService.getFrame(session.getSessionId(), 2);
        assertThat(storedFrame.getOriginalFileName()).isEqualTo("frame-2.jpg");
        assertThat(storedFrame.getContent()).isEqualTo("frame-two".getBytes());
    }
}
