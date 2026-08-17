package com.moneybags.customer.controller;

import com.moneybags.customer.dto.ExternalKycModels.KycContext;
import com.moneybags.customer.dto.ExternalKycModels.KycDecisionRequest;
import com.moneybags.customer.dto.ExternalKycModels.KycDecisionResult;
import com.moneybags.customer.service.ExternalKycSyncService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/v1/customers")
@RequiredArgsConstructor
public class InternalKycController {
    private final ExternalKycSyncService service;

    @GetMapping("/{cif}/kyc-context")
    public KycContext context(@PathVariable String cif) {
        return service.context(cif);
    }

    @PutMapping("/{cif}/kyc-decision")
    public KycDecisionResult synchronizeDecision(@PathVariable String cif,
                                                 @Valid @RequestBody KycDecisionRequest request) {
        return service.synchronize(cif, request);
    }
}
