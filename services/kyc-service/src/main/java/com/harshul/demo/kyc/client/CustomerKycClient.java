package com.harshul.demo.kyc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;

@FeignClient(name = "customer-service")
public interface CustomerKycClient {

    @GetMapping("/internal/v1/customers/{cif}/kyc-context")
    CustomerKycContext context(@PathVariable("cif") String cif);

    @PutMapping("/internal/v1/customers/{cif}/kyc-decision")
    void synchronizeDecision(@PathVariable("cif") String cif,
                             @RequestBody KycDecisionSyncRequest request);

    record CustomerKycContext(String cifNo, String customerStatus, String kycStatus) {
    }

    record KycDecisionSyncRequest(
            String sessionId,
            String status,
            String reviewerId,
            String reason,
            String remarks,
            Instant decidedAt) {
    }
}
