package com.moneybags.product.controller;

import com.moneybags.product.api.ApiModels.*;
import com.moneybags.product.entity.ProductStatus;
import com.moneybags.product.entity.ProductType;
import com.moneybags.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // --- Public catalogue --------------------------------------------------

    @Operation(summary = "Search the product catalogue")
    @GetMapping("/api/v1/products")
    public List<ProductDetail> search(@RequestParam(required = false) ProductStatus status,
                                      @RequestParam(required = false) ProductType productType) {
        return productService.search(status, productType);
    }

    @Operation(summary = "Read a product by its stable code")
    @GetMapping("/api/v1/products/{productCode}")
    public ProductDetail detail(@PathVariable String productCode) {
        return productService.detail(productCode);
    }

    @Operation(summary = "Create a product")
    @PostMapping("/api/v1/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetail create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @Operation(summary = "Update non-rate metadata")
    @PatchMapping("/api/v1/products/{productCode}")
    public ProductDetail update(@PathVariable String productCode,
                                @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(productCode, request);
    }

    @Operation(summary = "Activate a product")
    @PostMapping("/api/v1/products/{productCode}/activate")
    public ProductDetail activate(@PathVariable String productCode) {
        return productService.setStatus(productCode, ProductStatus.ACTIVE);
    }

    @Operation(summary = "Stop new sales without affecting existing accounts")
    @PostMapping("/api/v1/products/{productCode}/deactivate")
    public ProductDetail deactivate(@PathVariable String productCode) {
        return productService.setStatus(productCode, ProductStatus.INACTIVE);
    }

    // --- Charges and rules -------------------------------------------------

    @GetMapping("/api/v1/products/{productCode}/charges")
    public List<ChargeDetail> charges(@PathVariable String productCode) {
        return productService.charges(productCode);
    }

    @Operation(summary = "Replace the effective charge schedule")
    @PutMapping("/api/v1/products/{productCode}/charges")
    public List<ChargeDetail> replaceCharges(@PathVariable String productCode,
                                             @Valid @RequestBody List<ChargeRequest> requests) {
        return productService.replaceCharges(productCode, requests);
    }

    @GetMapping("/api/v1/products/{productCode}/rules")
    public List<RuleDetail> rules(@PathVariable String productCode) {
        return productService.rules(productCode);
    }

    @PostMapping("/api/v1/products/{productCode}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleDetail addRule(@PathVariable String productCode,
                              @Valid @RequestBody RuleRequest request) {
        return productService.addRule(productCode, request);
    }

    @DeleteMapping("/api/v1/products/{productCode}/rules/{ruleId}")
    public ResponseEntity<Void> retireRule(@PathVariable String productCode, @PathVariable Long ruleId) {
        productService.retireRule(productCode, ruleId);
        return ResponseEntity.noContent().build();
    }

    // --- Internal ----------------------------------------------------------

    /**
     * account-service calls this at opening and snapshots the result onto the account,
     * so a later rate change cannot rewrite the terms of an account already open.
     */
    @Operation(summary = "Resolve product terms for a business date")
    @GetMapping("/internal/v1/products/{productCode}/effective")
    public EffectiveProduct effective(@PathVariable String productCode,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return productService.effective(productCode, businessDate);
    }
}
