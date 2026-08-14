package com.moneybags.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "product-service")
public interface ProductClient {

    /**
     * Resolved at application submission and SNAPSHOTTED onto the account. Referencing
     * the product live would mean a later rate change silently rewrites the terms of
     * accounts that are already open.
     */
    @GetMapping("/internal/v1/products/{productCode}/effective")
    EffectiveProduct effective(@PathVariable("productCode") String productCode);

    record EffectiveProduct(
            String productCode,
            String productName,
            String productType,
            String currency,
            BigDecimal interestRate,
            BigDecimal minBalance,
            BigDecimal minOpeningDeposit,
            BigDecimal maxWithdrawalPerDay,
            Integer freeTxnPerMonth,
            Integer tenureMonths,
            BigDecimal overdraftLimit,
            boolean allowsOverdraft,
            boolean requiresFunding,
            Integer minAge,
            String status,
            String businessDate) {
    }
}
