package com.moneybags.customer.controller;

import com.moneybags.customer.service.KycDocumentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class KycDocumentHistoryController {

    private final KycDocumentHistoryService service;

    @GetMapping("/{cif}/kyc-rejections")
    public List<?> kycHistory(@PathVariable String cif) {
        return service.findByCustomer(cif);
    }
}