package com.moneybags.customer.service;

import com.moneybags.customer.dto.ExternalKycModels.KycContext;
import com.moneybags.customer.dto.ExternalKycModels.KycDecisionRequest;
import com.moneybags.customer.dto.ExternalKycModels.KycDecisionResult;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.enums.CustomerStatus;
import com.moneybags.customer.enums.KycStatus;
import com.moneybags.customer.exception.ResourceNotFoundException;
import com.moneybags.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ExternalKycSyncService {
    private final CustomerRepository customers;
    private final CustomerEventOutboxService events;

    @Transactional(readOnly = true)
    public KycContext context(String cif) {
        Customer customer = customer(cif);
        return new KycContext(customer.getCifNo(), customer.getStatus().name(), customer.getKycStatus().name());
    }

    public KycDecisionResult synchronize(String cif, KycDecisionRequest request) {
        Customer customer = customer(cif);
        KycStatus requestedStatus = KycStatus.valueOf(request.status());
        Instant decidedAt = request.decidedAt() == null ? Instant.now() : request.decidedAt();

        boolean sameDecision = request.sessionId().equals(customer.getExternalKycSessionId())
                && requestedStatus.name().equals(customer.getExternalKycDecision());
        if (sameDecision) {
            return result(customer, request.sessionId(), false);
        }

        if (customer.getExternalKycDecidedAt() != null
                && decidedAt.isBefore(customer.getExternalKycDecidedAt())) {
            return result(customer, request.sessionId(), false);
        }

        if (requestedStatus == KycStatus.REJECTED
                && !(request.sessionId().equals(customer.getExternalKycSessionId())
                && "REJECTED".equals(customer.getExternalKycDecision()))) {
            customer.setKycFailureCount((customer.getKycFailureCount() == null
                    ? 0 : customer.getKycFailureCount()) + 1);
        }

        customer.setKycStatus(requestedStatus);
        if (requestedStatus == KycStatus.VERIFIED
                && customer.getStatus() != CustomerStatus.BLOCKED
                && customer.getStatus() != CustomerStatus.DECEASED) {
            customer.setStatus(CustomerStatus.ACTIVE);
        } else if (requestedStatus == KycStatus.REJECTED
                && customer.getStatus() == CustomerStatus.ACTIVE) {
            customer.setStatus(CustomerStatus.INACTIVE);
        }
        customer.setExternalKycSessionId(request.sessionId());
        customer.setExternalKycDecision(requestedStatus.name());
        customer.setExternalKycDecidedAt(decidedAt);
        customers.save(customer);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cifNo", cif);
        payload.put("kycSessionId", request.sessionId());
        payload.put("status", requestedStatus.name());
        payload.put("reviewerId", request.reviewerId());
        payload.put("reason", request.reason());
        if (request.remarks() != null) payload.put("remarks", request.remarks());
        payload.put("decidedAt", decidedAt.toString());
        events.record("CUSTOMER", cif,
                requestedStatus == KycStatus.VERIFIED ? "KycVerified" : "KycRejected", payload);

        return result(customer, request.sessionId(), true);
    }

    private Customer customer(String cif) {
        return customers.findById(cif)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + cif));
    }

    private KycDecisionResult result(Customer customer, String sessionId, boolean applied) {
        return new KycDecisionResult(customer.getCifNo(), sessionId, customer.getKycStatus().name(), applied);
    }
}
