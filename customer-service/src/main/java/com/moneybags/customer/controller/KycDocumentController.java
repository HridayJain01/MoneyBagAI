package com.moneybags.customer.controller;

import com.moneybags.customer.dto.CustomerOperations.KycDecision;
import com.moneybags.customer.dto.CustomerOperations.KycSubmit;
import com.moneybags.customer.service.KycDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class KycDocumentController {

    private final KycDocumentService service;

    @PostMapping("/{cif}/kyc-documents")
    public ResponseEntity<?> kyc(
            @PathVariable String cif,
            @Valid @RequestBody KycSubmit request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.submit(cif, request));
    }

    @PutMapping("/{cif}/kyc-documents/{documentId}/assignment/{empId}")
    public Object assignKyc(
            @PathVariable String cif,
            @PathVariable Long documentId,
            @PathVariable Long empId
    ) {
        return service.assign(cif, documentId, empId);
    }

    @PatchMapping("/{cif}/kyc-documents/{documentId}/decision")
    public Object kycDecision(
            @PathVariable String cif,
            @PathVariable Long documentId,
            @Valid @RequestBody KycDecision request
    ) {
        return service.decide(cif, documentId, request);
    }

    @GetMapping("/kyc/pending")
    public List<?> pendingKyc() {
        return service.findPending();
    }

    @GetMapping("/kyc/re-kyc-required")
    public List<?> reKycRequired() {
        return service.findReKycRequired();
    }

    @GetMapping("/{cif}/kyc-documents")
    public List<?> documents(@PathVariable String cif) {
        return service.findByCustomer(cif);
    }

    @PostMapping("/kyc/expiry-alerts/process")
    public Map<String, Integer> processExpiryAlerts(
            @RequestParam(required = false) LocalDate asOfDate
    ) {
        return Map.of("alertsCreated", service.processExpiryAlerts(asOfDate));
    }
}
