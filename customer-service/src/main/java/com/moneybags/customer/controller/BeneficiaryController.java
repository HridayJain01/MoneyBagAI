package com.moneybags.customer.controller;

import com.moneybags.customer.dto.CustomerOperations.BeneficiaryRequest;
import com.moneybags.customer.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService service;

    @PostMapping("/{cif}/beneficiaries")
    public ResponseEntity<?> beneficiary(
            @PathVariable String cif,
            @Valid @RequestBody BeneficiaryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.add(cif, request));
    }

    @PutMapping("/{cif}/beneficiaries/{id}")
    public Object updateBeneficiary(
            @PathVariable String cif,
            @PathVariable Long id,
            @Valid @RequestBody BeneficiaryRequest request
    ) {
        return service.update(cif, id, request);
    }

    @PostMapping("/{cif}/beneficiaries/{id}/activate")
    public Object activate(
            @PathVariable String cif,
            @PathVariable Long id
    ) {
        return service.activate(cif, id);
    }

    @GetMapping("/{cif}/beneficiaries/{id}/eligibility")
    public Object eligibility(
            @PathVariable String cif,
            @PathVariable Long id
    ) {
        return service.eligibility(cif, id);
    }

    @PatchMapping("/{cif}/beneficiaries/{id}/block")
    public Object block(
            @PathVariable String cif,
            @PathVariable Long id,
            @RequestParam boolean blocked
    ) {
        return service.setBlocked(cif, id, blocked);
    }

    @DeleteMapping("/{cif}/beneficiaries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBeneficiary(
            @PathVariable String cif,
            @PathVariable Long id
    ) {
        service.remove(cif, id);
    }

    @GetMapping("/{cif}/beneficiaries")
    public List<?> beneficiaries(
            @PathVariable String cif,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String beneficiaryType
    ) {
        return service.findByCustomer(cif, status, beneficiaryType);
    }
}
