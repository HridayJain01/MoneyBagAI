package com.moneybags.product.api;

import com.moneybags.product.entity.ProductType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    /**
     * The terms snapshot account-service copies onto an account at opening.
     *
     * <p>Copied rather than referenced on purpose: a later rate change must not silently
     * rewrite the terms of accounts already open.
     */
    public record EffectiveProduct(
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
            LocalDate businessDate) {
    }

    public record ProductDetail(
            String productCode,
            String productName,
            String productType,
            String description,
            String currency,
            BigDecimal interestRate,
            BigDecimal minBalance,
            BigDecimal minOpeningDeposit,
            BigDecimal maxWithdrawalPerDay,
            Integer freeTxnPerMonth,
            Integer tenureMonths,
            boolean allowsOverdraft,
            boolean requiresFunding,
            Integer minAge,
            String status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Instant createdAt,
            List<ChargeDetail> charges,
            List<RuleDetail> rules) {
    }

    public record ChargeDetail(Long chargeId, String chargeType, BigDecimal amount, String frequency) {
    }

    public record RuleDetail(Long ruleId, String ruleKey, String ruleValue, String dataType, boolean active) {
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 30) String productCode,
            @NotBlank @Size(max = 120) String productName,
            @NotNull ProductType productType,
            @Size(max = 500) String description,
            @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @DecimalMin("0.0") BigDecimal interestRate,
            @NotNull @DecimalMin("0.0") BigDecimal minBalance,
            @DecimalMin("0.0") BigDecimal minOpeningDeposit,
            @DecimalMin("0.0") BigDecimal maxWithdrawalPerDay,
            @Min(0) Integer freeTxnPerMonth,
            @Min(1) Integer tenureMonths,
            Boolean allowsOverdraft,
            Boolean requiresFunding,
            @Min(0) Integer minAge,
            LocalDate effectiveFrom) {
    }

    public record UpdateProductRequest(
            @Size(max = 120) String productName,
            @Size(max = 500) String description,
            @DecimalMin("0.0") BigDecimal interestRate,
            @DecimalMin("0.0") BigDecimal minBalance,
            @DecimalMin("0.0") BigDecimal minOpeningDeposit,
            @DecimalMin("0.0") BigDecimal maxWithdrawalPerDay,
            @Min(0) Integer freeTxnPerMonth,
            @Min(0) Integer minAge,
            LocalDate effectiveTo) {
    }

    public record ChargeRequest(
            @NotBlank @Size(max = 50) String chargeType,
            @NotNull @DecimalMin("0.0") BigDecimal amount,
            @NotBlank @Size(max = 30) String frequency) {
    }

    public record RuleRequest(
            @NotBlank @Size(max = 60) String ruleKey,
            @NotBlank @Size(max = 255) String ruleValue,
            @NotBlank @Size(max = 20) String dataType) {
    }

    public record ErrorResponse(Instant timestamp, int status, String code, String message, String path) {
    }
}
