package com.moneybags.customer.service.impl;

import com.moneybags.customer.dto.CustomerOperations.KycDecision;
import com.moneybags.customer.dto.CustomerOperations.KycSubmit;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.entity.KycDocument;
import com.moneybags.customer.enums.DocumentVerifyStatus;
import com.moneybags.customer.enums.KycStatus;
import com.moneybags.customer.exception.ConflictException;
import com.moneybags.customer.exception.ResourceNotFoundException;
import com.moneybags.customer.repository.CustomerRepository;
import com.moneybags.customer.repository.KycDocumentRepository;
import com.moneybags.customer.service.CustomerEventOutboxService;
import com.moneybags.customer.service.KycDocumentHistoryService;
import com.moneybags.customer.service.KycDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class KycDocumentServiceImpl implements KycDocumentService {

    private static final int RE_KYC_WARNING_DAYS = 30;

    private final KycDocumentRepository kycDocumentRepository;
    private final CustomerRepository customerRepository;
    private final KycDocumentHistoryService historyService;
    private final CustomerEventOutboxService eventOutboxService;

    @Override
    @Transactional(readOnly = true)
    public KycDocument getByIdAndCif(String cif, Long documentId) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found: " + documentId));
        if (!document.getCustomer().getCifNo().equals(cif)) {
            throw new ResourceNotFoundException("KYC document not found: " + documentId);
        }
        return document;
    }

    @Override
    public KycDocument submit(String cif, KycSubmit request) {
        Customer customer = getCustomer(cif);
        customer.setKycStatus(KycStatus.PENDING);

        String normalizedDocumentNumber = request.docNumber().trim().toUpperCase();
        KycDocument document = KycDocument.builder()
                .customer(customer)
                .docType(request.docType().trim().toUpperCase())
                .docNumber(mask(normalizedDocumentNumber))
                .documentNumberHash(sha256(normalizedDocumentNumber))
                .expiryDate(request.expiryDate())
                .filePath(request.filePath().trim())
                .verifyStatus(DocumentVerifyStatus.PENDING)
                .build();

        customerRepository.save(customer);
        return kycDocumentRepository.save(document);
    }

    @Override
    public KycDocument assign(String cif, Long documentId, Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new ConflictException("A valid employee ID is required");
        }
        KycDocument document = getByIdAndCif(cif, documentId);
        if (document.getVerifyStatus() != DocumentVerifyStatus.PENDING) {
            throw new ConflictException("Only pending KYC documents can be assigned");
        }
        document.setAssignedToEmpId(employeeId);
        return kycDocumentRepository.save(document);
    }

    @Override
    public KycDocument decide(String cif, Long documentId, KycDecision request) {
        validateDecision(request);
        KycDocument document = getByIdAndCif(cif, documentId);
        if (document.getVerifyStatus() != DocumentVerifyStatus.PENDING) {
            if (document.getVerifyStatus() == request.status()) {
                return document;
            }
            throw new ConflictException("KYC document has already been decided");
        }

        document.setVerifyStatus(request.status());
        document.setVerifiedByEmpId(request.employeeId());
        document.setVerifiedAt(LocalDateTime.now());
        document.setRejectionReason(request.status() == DocumentVerifyStatus.REJECTED
                ? request.rejectionReason().trim()
                : null);
        KycDocument savedDocument = kycDocumentRepository.save(document);

        Customer customer = document.getCustomer();
        if (request.status() == DocumentVerifyStatus.REJECTED) {
            handleRejection(customer, savedDocument, request);
            recordDecisionEvent("KycRejected", customer, savedDocument, request.rejectionReason());
        } else {
            boolean hasPendingDocument = kycDocumentRepository
                    .existsByCustomerCifNoAndVerifyStatus(cif, DocumentVerifyStatus.PENDING);
            customer.setKycStatus(hasPendingDocument ? KycStatus.PENDING : KycStatus.VERIFIED);
            if (!hasPendingDocument) {
                recordDecisionEvent("KycVerified", customer, savedDocument, null);
            }
        }
        customerRepository.save(customer);
        return savedDocument;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycDocument> findPending() {
        return kycDocumentRepository.findByVerifyStatus(DocumentVerifyStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycDocument> findReKycRequired() {
        return kycDocumentRepository.findByExpiryDateLessThanEqualOrderByExpiryDateAsc(
                LocalDate.now().plusDays(RE_KYC_WARNING_DAYS));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycDocument> findByCustomer(String cif) {
        getCustomer(cif);
        return kycDocumentRepository.findByCustomerCifNo(cif);
    }

    @Override
    public int processExpiryAlerts(LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate == null ? LocalDate.now() : asOfDate;
        List<KycDocument> documents = kycDocumentRepository
                .findUnalertedExpiringThrough(effectiveDate.plusDays(RE_KYC_WARNING_DAYS));

        for (KycDocument document : documents) {
            boolean expired = document.getExpiryDate().isBefore(effectiveDate);
            if (expired) {
                document.getCustomer().setKycStatus(KycStatus.EXPIRED);
                customerRepository.save(document.getCustomer());
            }
            document.setExpiryAlertedAt(LocalDateTime.now());
            kycDocumentRepository.save(document);
            eventOutboxService.record(
                    "KYC_DOCUMENT",
                    document.getDocId().toString(),
                    expired ? "KycDocumentExpired" : "KycDocumentExpiring",
                    Map.of(
                            "cifNo", document.getCustomer().getCifNo(),
                            "documentId", document.getDocId(),
                            "documentType", document.getDocType(),
                            "expiryDate", document.getExpiryDate().toString()
                    )
            );
        }
        return documents.size();
    }

    private Customer getCustomer(String cif) {
        return customerRepository.findById(cif)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + cif));
    }

    private void validateDecision(KycDecision request) {
        if (request.status() == null || request.status() == DocumentVerifyStatus.PENDING) {
            throw new ConflictException("KYC decision must be VERIFIED or REJECTED");
        }
        if (request.employeeId() == null || request.employeeId() <= 0) {
            throw new ConflictException("A valid verifying employee ID is required");
        }
        if (request.status() == DocumentVerifyStatus.REJECTED
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new ConflictException("Rejection reason is required");
        }
    }

    private void handleRejection(Customer customer, KycDocument document, KycDecision request) {
        customer.setKycStatus(KycStatus.REJECTED);
        int newFailureCount = (customer.getKycFailureCount() == null ? 0 : customer.getKycFailureCount()) + 1;
        customer.setKycFailureCount(newFailureCount);
        historyService.recordRejection(
                customer.getCifNo(),
                document.getDocId(),
                request.rejectionReason().trim(),
                request.employeeId(),
                newFailureCount
        );
    }

    private void recordDecisionEvent(
            String eventType,
            Customer customer,
            KycDocument document,
            String rejectionReason
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cifNo", customer.getCifNo());
        payload.put("documentId", document.getDocId());
        payload.put("documentType", document.getDocType());
        payload.put("verifiedByEmployeeId", document.getVerifiedByEmpId());
        if (rejectionReason != null) payload.put("rejectionReason", rejectionReason);
        eventOutboxService.record("CUSTOMER", customer.getCifNo(), eventType, payload);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String mask(String value) {
        int visibleCharacters = Math.min(4, value.length());
        return "*".repeat(Math.max(4, value.length() - visibleCharacters))
                + value.substring(value.length() - visibleCharacters);
    }
}
