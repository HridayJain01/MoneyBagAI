package com.harshul.demo.kyc.controller;
import com.harshul.demo.kyc.dto.request.CreateKycSessionRequest;
import com.harshul.demo.kyc.dto.request.ManualDecisionRequest;
import com.harshul.demo.kyc.dto.response.*;
import com.harshul.demo.kyc.entity.CapturedFrameEntity;
import com.harshul.demo.kyc.entity.DocumentType;
import com.harshul.demo.kyc.entity.KycDocumentEntity;
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
@RequestMapping("/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<KycSessionResponse> createSession(
            @RequestBody CreateKycSessionRequest request
    ) {
        return ResponseEntity.ok(
                kycService.createSession(request)
        );
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<KycSessionResponse> getSession(
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(kycService.getSession(sessionId));
    }

    @PostMapping(
            value = "/sessions/{sessionId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<KycDocumentResponse> uploadDocument(
            @PathVariable String sessionId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        DocumentType type = DocumentType.valueOf(documentType.toUpperCase());
        return ResponseEntity.ok(
                kycService.uploadDocument(sessionId, type, file)
        );
    }

    @GetMapping("/customers/{externalUserId}/sessions/pending")
    public ResponseEntity<List<KycSessionResponse>> getPendingSessions(@PathVariable String externalUserId) {
        return ResponseEntity.ok(kycService.findPendingSessions(externalUserId));
    }
    @GetMapping("/customers/{externalUserId}/sessions")
    public ResponseEntity<List<KycSessionResponse>> getSessions(
            @PathVariable String externalUserId,
            @RequestParam(required = false) String documentType
    ) {
        DocumentType type = documentType == null || documentType.isBlank() ? null : DocumentType.valueOf(documentType.toUpperCase());
        return ResponseEntity.ok(kycService.findSessions(externalUserId, type));
    }

    @GetMapping("/sessions/{sessionId}/documents/{documentType}")
    public ResponseEntity<byte[]> getDocument(
            @PathVariable String sessionId,
            @PathVariable String documentType,
            @RequestParam(defaultValue = "false") boolean inline
    ) throws IOException {
        DocumentType type = DocumentType.valueOf(documentType.toUpperCase());
        KycDocumentEntity document = kycService.getDocument(sessionId, type);

        return ResponseEntity.ok()
                .contentType(inline ? MediaType.APPLICATION_PDF : MediaType.parseMediaType(
                        Optional.ofNullable(document.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                ))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (inline ? "inline" : "attachment") + "; filename=\"" + document.getOriginalFileName() + "\"")
                .body(document.getContent());
    }

    @PostMapping(
            value = "/sessions/{sessionId}/frames",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FrameUploadResponse> uploadFrames(
            @PathVariable String sessionId,
            @RequestParam("frames") List<MultipartFile> frames
    ) throws IOException {
        return ResponseEntity.ok(
                kycService.processFrames(sessionId, frames)
        );
    }

    @GetMapping("/sessions/{sessionId}/frames")
    public ResponseEntity<List<CapturedFrameResponse>> getFrames(
            @PathVariable String sessionId
    ) throws IOException {
        return ResponseEntity.ok(kycService.getFrames(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/frames/{frameNumber}")
    public ResponseEntity<byte[]> getFrame(
            @PathVariable String sessionId,
            @PathVariable int frameNumber
    ) throws IOException {
        CapturedFrameEntity frame = kycService.getFrame(sessionId, frameNumber);

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


    @GetMapping("/sessions/{sessionId}/result")
    public ResponseEntity<KycVerificationResultResponse> getResult(
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(
                kycService.getResult(sessionId)
        );
    }

    @PostMapping("/sessions/{sessionId}/approve")
    public ResponseEntity<KycDecisionResponse> approve(
            @PathVariable String sessionId,
            @RequestBody ManualDecisionRequest request
    ) {
        return ResponseEntity.ok(
                kycService.approve(sessionId, request)
        );
    }

    @PostMapping("/sessions/{sessionId}/reject")
    public ResponseEntity<KycDecisionResponse> reject(
            @PathVariable String sessionId,
            @RequestBody ManualDecisionRequest request
    ) {
        return ResponseEntity.ok(
                kycService.reject(sessionId, request)
        );
    }

    private String frameFileName(CapturedFrameEntity frame) {
        if (frame.getOriginalFileName() != null && !frame.getOriginalFileName().isBlank()) {
            return frame.getOriginalFileName();
        }
        return "frame-" + frame.getFrameNumber() + ".jpg";
    }
}
