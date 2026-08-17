package com.harshul.demo.kyc.controller;
import com.harshul.demo.kyc.dto.request.CreateKycSessionRequest;
import com.harshul.demo.kyc.dto.request.ManualDecisionRequest;
import com.harshul.demo.kyc.dto.response.*;
import com.harshul.demo.kyc.entity.CapturedFrameEntity;
import com.harshul.demo.kyc.entity.DocumentType;
import com.harshul.demo.kyc.entity.KycDocumentEntity;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import com.harshul.demo.kyc.service.KycService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<KycSessionResponse> createSession(
            @Valid @RequestBody CreateKycSessionRequest request
    ) {
        return ResponseEntity.ok(
                kycService.createSession(request)
        );
    }

    @GetMapping("/sessions/{kycSessionId}")
    public ResponseEntity<KycSessionResponse> getSession(
            @PathVariable String kycSessionId
    ) {
        return ResponseEntity.ok(kycService.getSession(kycSessionId));
    }

    @PostMapping(
            value = "/sessions/{kycSessionId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<KycDocumentResponse> uploadDocument(
            @PathVariable String kycSessionId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        DocumentType type = DocumentType.valueOf(documentType.toUpperCase());
        return ResponseEntity.ok(
                kycService.uploadDocument(kycSessionId, type, file)
        );
    }

    @GetMapping("/customers/{cif}/sessions/pending")
    public ResponseEntity<List<KycSessionResponse>> getPendingSessions(@PathVariable String cif) {
        return ResponseEntity.ok(kycService.findPendingSessions(cif));
    }

    @GetMapping("/sessions/{kycSessionId}/documents/{documentType}")
    public ResponseEntity<byte[]> getDocument(
            @PathVariable String kycSessionId,
            @PathVariable String documentType
    ) throws IOException {
        DocumentType type = DocumentType.valueOf(documentType.toUpperCase());
        KycDocumentEntity document = kycService.getDocument(kycSessionId, type);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        Optional.ofNullable(document.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getOriginalFileName() + "\""
                )
                .body(document.getContent());
    }

    @PostMapping(
            value = "/sessions/{kycSessionId}/frames",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FrameUploadResponse> uploadFrames(
            @PathVariable String kycSessionId,
            @RequestParam("frames") List<MultipartFile> frames
    ) throws IOException {
        return ResponseEntity.ok(
                kycService.processFrames(kycSessionId, frames)
        );
    }

    @GetMapping("/sessions/{kycSessionId}/frames")
    public ResponseEntity<List<CapturedFrameResponse>> getFrames(
            @PathVariable String kycSessionId
    ) throws IOException {
        return ResponseEntity.ok(kycService.getFrames(kycSessionId));
    }

    @GetMapping("/sessions/{kycSessionId}/frames/{frameNumber}")
    public ResponseEntity<byte[]> getFrame(
            @PathVariable String kycSessionId,
            @PathVariable int frameNumber
    ) throws IOException {
        CapturedFrameEntity frame = kycService.getFrame(kycSessionId, frameNumber);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        Optional.ofNullable(frame.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                ))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + frameFileName(frame) + "\""
                )
                .body(frame.getContent());
    }


    @GetMapping("/sessions/{kycSessionId}/result")
    public ResponseEntity<KycVerificationResultResponse> getResult(
            @PathVariable String kycSessionId
    ) {
        return ResponseEntity.ok(
                kycService.getResult(kycSessionId)
        );
    }

    @PostMapping("/sessions/{kycSessionId}/approve")
    public ResponseEntity<KycDecisionResponse> approve(
            @PathVariable String kycSessionId,
            @Parameter(hidden = true) @RequestHeader("X-Employee-Id") String reviewerId,
            @Valid @RequestBody ManualDecisionRequest request
    ) {
        return ResponseEntity.ok(
                kycService.approve(kycSessionId, reviewerId, request)
        );
    }

    @PostMapping("/sessions/{kycSessionId}/reject")
    public ResponseEntity<KycDecisionResponse> reject(
            @PathVariable String kycSessionId,
            @Parameter(hidden = true) @RequestHeader("X-Employee-Id") String reviewerId,
            @Valid @RequestBody ManualDecisionRequest request
    ) {
        return ResponseEntity.ok(
                kycService.reject(kycSessionId, reviewerId, request)
        );
    }

    private String frameFileName(CapturedFrameEntity frame) {
        if (frame.getOriginalFileName() != null && !frame.getOriginalFileName().isBlank()) {
            return frame.getOriginalFileName();
        }
        return "frame-" + frame.getFrameNumber() + ".jpg";
    }
}
