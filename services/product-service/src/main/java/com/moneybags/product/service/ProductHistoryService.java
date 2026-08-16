package com.moneybags.product.service;

import com.moneybags.product.dto.ChargeDetail;
import com.moneybags.product.dto.ProductVersionDetail;
import com.moneybags.product.dto.RuleDetail;
import com.moneybags.product.entity.*;
import com.moneybags.product.repository.*;
import com.moneybags.product.support.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductHistoryService {

    private static final String OVERDRAFT_LIMIT_RULE = "OVERDRAFT_LIMIT";

    private final ProductRepository products;
    private final ProductChargeRepository charges;
    private final ProductRuleRepository rules;
    private final ProductVersionRepository versions;
    private final ProductVersionChargeRepository versionCharges;
    private final ProductVersionRuleRepository versionRules;

    @Transactional
    public void ensureBaseline(Product product) {
        if (latest(product.getProductCode()).isEmpty()) {
            appendVersion(product);
        }
    }

    @Transactional
    public ProductVersion appendVersion(Product product) {
        int versionNumber = latest(product.getProductCode())
                .map(existing -> existing.getVersionNumber() + 1)
                .orElse(1);
        ProductVersion version = versions.saveAndFlush(ProductVersion.builder()
                .productCode(product.getProductCode())
                .versionNumber(versionNumber)
                .productName(product.getProductName())
                .productType(product.getProductType())
                .description(product.getDescription())
                .currency(product.getCurrency())
                .interestRate(product.getInterestRate())
                .minBalance(product.getMinBalance())
                .minOpeningDeposit(product.getMinOpeningDeposit())
                .maxWithdrawalPerDay(product.getMaxWithdrawalPerDay())
                .freeTxnPerMonth(product.getFreeTxnPerMonth())
                .tenureMonths(product.getTenureMonths())
                .allowsOverdraft(product.getAllowsOverdraft())
                .requiresFunding(product.getRequiresFunding())
                .minAge(product.getMinAge())
                .status(product.getStatus())
                .effectiveFrom(product.getEffectiveFrom())
                .effectiveTo(product.getEffectiveTo())
                .build());

        versionCharges.saveAll(charges.findByProductCodeOrderByChargeType(product.getProductCode()).stream()
                .map(charge -> ProductVersionCharge.builder()
                        .productVersionId(version.getProductVersionId())
                        .chargeType(charge.getChargeType())
                        .amount(charge.getAmount())
                        .frequency(charge.getFrequency())
                        .build())
                .toList());
        versionRules.saveAll(rules.findByProductCodeOrderByRuleKey(product.getProductCode()).stream()
                .map(rule -> ProductVersionRule.builder()
                        .productVersionId(version.getProductVersionId())
                        .ruleKey(rule.getRuleKey())
                        .ruleValue(rule.getRuleValue())
                        .dataType(rule.getDataType())
                        .active(rule.getActive())
                        .build())
                .toList());
        return version;
    }

    @Transactional(readOnly = true)
    public List<ProductVersionDetail> history(String productCode) {
        requireProduct(productCode);
        return versions.findByProductCodeOrderByVersionNumberDesc(productCode).stream()
                .map(this::toDetail).toList();
    }

    @Transactional(readOnly = true)
    public ProductVersionDetail version(String productCode, Integer versionNumber) {
        requireProduct(productCode);
        return toDetail(versions.findByProductCodeAndVersionNumber(productCode, versionNumber)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_VERSION_NOT_FOUND",
                        "No version " + versionNumber + " exists for product " + productCode)));
    }

    @Transactional(readOnly = true)
    public ProductVersionDetail asOf(String productCode, LocalDate businessDate) {
        requireProduct(productCode);
        return toDetail(resolveAsOf(productCode, businessDate)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_VERSION_NOT_FOUND",
                        "No version of " + productCode + " applies on " + businessDate)));
    }

    @Transactional(readOnly = true)
    public Optional<ProductVersion> resolveAsOf(String productCode, LocalDate businessDate) {
        return versions
                .findByProductCodeAndEffectiveFromLessThanEqualOrderByVersionNumberDesc(
                        productCode, businessDate)
                .stream()
                .filter(version -> version.appliesOn(businessDate))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<ProductVersion> latest(String productCode) {
        return versions.findFirstByProductCodeOrderByVersionNumberDesc(productCode);
    }

    @Transactional(readOnly = true)
    public BigDecimal overdraftLimit(ProductVersion version) {
        if (!Boolean.TRUE.equals(version.getAllowsOverdraft())) return BigDecimal.ZERO;
        return versionRules.findByProductVersionIdOrderByRuleKey(version.getProductVersionId()).stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getActive()))
                .filter(rule -> OVERDRAFT_LIMIT_RULE.equals(rule.getRuleKey()))
                .findFirst()
                .map(rule -> new BigDecimal(rule.getRuleValue()))
                .orElse(BigDecimal.ZERO);
    }

    public void ensureChronological(String productCode, LocalDate effectiveFrom) {
        latest(productCode).ifPresent(existing -> {
            if (effectiveFrom.isBefore(existing.getEffectiveFrom())) {
                throw ApiException.invalid("PRODUCT_VERSION_BACKDATED",
                        "A new version cannot start before version " + existing.getVersionNumber()
                                + " on " + existing.getEffectiveFrom());
            }
        });
    }

    private ProductVersionDetail toDetail(ProductVersion version) {
        List<ChargeDetail> chargeDetails = versionCharges
                .findByProductVersionIdOrderByChargeType(version.getProductVersionId()).stream()
                .map(charge -> new ChargeDetail(charge.getVersionChargeId(), charge.getChargeType(),
                        charge.getAmount(), charge.getFrequency()))
                .toList();
        List<RuleDetail> ruleDetails = versionRules
                .findByProductVersionIdOrderByRuleKey(version.getProductVersionId()).stream()
                .map(rule -> new RuleDetail(rule.getVersionRuleId(), rule.getRuleKey(),
                        rule.getRuleValue(), rule.getDataType(), Boolean.TRUE.equals(rule.getActive())))
                .toList();
        return new ProductVersionDetail(
                version.getProductVersionId(), version.getVersionNumber(), version.getProductCode(),
                version.getProductName(), version.getProductType().name(), version.getDescription(),
                version.getCurrency(), version.getInterestRate(), version.getMinBalance(),
                version.getMinOpeningDeposit(), version.getMaxWithdrawalPerDay(),
                version.getFreeTxnPerMonth(), version.getTenureMonths(),
                Boolean.TRUE.equals(version.getAllowsOverdraft()),
                Boolean.TRUE.equals(version.getRequiresFunding()), version.getMinAge(),
                version.getStatus().name(), version.getEffectiveFrom(), version.getEffectiveTo(),
                version.getRecordedAt(), chargeDetails, ruleDetails);
    }

    private void requireProduct(String productCode) {
        if (!products.existsById(productCode)) {
            throw ApiException.notFound("PRODUCT_NOT_FOUND", "No product with code " + productCode);
        }
    }
}
