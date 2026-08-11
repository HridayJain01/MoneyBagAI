package com.moneybags.customer.controller;

import com.moneybags.customer.service.BeneficiaryHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class BeneficiaryHistoryController {

    private final BeneficiaryHistoryService service;

    @GetMapping("/beneficiaries/{id}/history")
    public List<?> history(@PathVariable Long id) {
        return service.findByBeneficiaryId(id);
    }
}