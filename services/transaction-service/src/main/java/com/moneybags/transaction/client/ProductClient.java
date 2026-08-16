package com.moneybags.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDate;

@FeignClient(name = "product-service")
public interface ProductClient {

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
            LocalDate businessDate,
            Long productVersionId,
            Integer versionNumber) {
    }
}
