package com.moneybags.product.service;

import com.moneybags.product.dto.*;
import com.moneybags.product.entity.ProductStatus;
import com.moneybags.product.entity.ProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** Stable facade used by the controller while responsibilities live in focused services. */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductCatalogService catalog;
    private final ProductHistoryService history;
    private final ProductTermsService terms;

    public EffectiveProduct effective(String productCode, LocalDate businessDate) {
        return catalog.effective(productCode, businessDate);
    }

    public List<ProductDetail> search(ProductStatus status, ProductType productType) {
        return catalog.search(status, productType);
    }

    public ProductDetail detail(String productCode) {
        return catalog.detail(productCode);
    }

    public ProductDetail create(CreateProductRequest request) {
        return catalog.create(request);
    }

    public ProductDetail update(String productCode, UpdateProductRequest request) {
        return catalog.update(productCode, request);
    }

    public ProductDetail setStatus(String productCode, ProductStatus status) {
        return catalog.setStatus(productCode, status);
    }

    public List<ChargeDetail> replaceCharges(String productCode, List<ChargeRequest> requests) {
        return terms.replaceCharges(productCode, requests);
    }

    public List<ChargeDetail> charges(String productCode) {
        return terms.charges(productCode);
    }

    public List<RuleDetail> rules(String productCode) {
        return terms.rules(productCode);
    }

    public RuleDetail addRule(String productCode, RuleRequest request) {
        return terms.addRule(productCode, request);
    }

    public void retireRule(String productCode, Long ruleId) {
        terms.retireRule(productCode, ruleId);
    }

    public List<ProductVersionDetail> history(String productCode) {
        return history.history(productCode);
    }

    public ProductVersionDetail version(String productCode, Integer versionNumber) {
        return history.version(productCode, versionNumber);
    }

    public ProductVersionDetail asOf(String productCode, LocalDate businessDate) {
        return history.asOf(productCode, businessDate);
    }
}
