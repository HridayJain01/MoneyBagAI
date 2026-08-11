package com.moneybags.customer.controller;

import com.moneybags.customer.dto.CustomerOperations.Update;
import com.moneybags.customer.dto.CustomerOperations.CommunicationPreferences;
import com.moneybags.customer.dto.CustomerOperations.RiskUpdate;
import com.moneybags.customer.dto.CustomerEligibilityResponse;
import com.moneybags.customer.enums.CustomerStatus;
import com.moneybags.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerManagementController {

    private final CustomerService service;

    @PatchMapping("/{cif}")
    public Object update(
            @PathVariable String cif,
            @Valid @RequestBody Update request
    ) {
        return service.update(cif, request);
    }

    @GetMapping("/{cif}/summary")
    public Map<String, Object> summary(@PathVariable String cif) {
        return service.summary(cif);
    }

    @GetMapping("/{cif}/completeness")
    public Map<String, Integer> completeness(@PathVariable String cif) {
        return service.completeness(cif);
    }

    @PatchMapping("/{cif}/status")
    public Object status(
            @PathVariable String cif,
            @RequestParam CustomerStatus status
    ) {
        return service.setStatus(cif, status);
    }

    @PutMapping("/{cif}/relationship-manager/{empId}")
    public Object manager(
            @PathVariable String cif,
            @PathVariable Long empId
    ) {
        return service.assignManager(cif, empId);
    }

    @DeleteMapping("/{cif}/relationship-manager")
    public Object removeManager(@PathVariable String cif) {
        return service.removeManager(cif);
    }

    @GetMapping("/relationship-manager/{empId}")
    public List<?> managerCustomers(@PathVariable Long empId) {
        return service.findByManager(empId);
    }

    @PutMapping("/{cif}/communication-preferences")
    public Object updateCommunicationPreferences(
            @PathVariable String cif,
            @Valid @RequestBody CommunicationPreferences request
    ) {
        return service.updateCommunicationPreferences(cif, request);
    }

    @GetMapping("/{cif}/communication-preferences")
    public Map<String, Object> communicationPreferences(@PathVariable String cif) {
        return service.communicationPreferences(cif);
    }

    @PutMapping("/{cif}/risk-classification")
    public Object classifyRisk(
            @PathVariable String cif,
            @Valid @RequestBody RiskUpdate request
    ) {
        return service.classifyRisk(cif, request);
    }

    @GetMapping("/{cif}/eligibility")
    public CustomerEligibilityResponse eligibility(@PathVariable String cif) {
        return service.eligibility(cif);
    }
}
