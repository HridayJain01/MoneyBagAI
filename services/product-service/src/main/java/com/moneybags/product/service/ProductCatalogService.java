package com.moneybags.product.service;

import com.moneybags.product.dto.*;
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
public class ProductCatalogService {

    private static final String OVERDRAFT_LIMIT_RULE = "OVERDRAFT_LIMIT";

    private final ProductRepository products;
    private final ProductChargeRepository charges;
    private final ProductRuleRepository rules;
    private final ProductHistoryService history;

    @Transactional(readOnly = true)
    public EffectiveProduct effective(String productCode, LocalDate businessDate) {
        Product product = require(productCode);
        LocalDate asOf = businessDate == null ? LocalDate.now() : businessDate;
        Optional<ProductVersion> version = history.resolveAsOf(productCode, asOf);
        if (version.isPresent()) {
            if (version.get().getStatus() != ProductStatus.ACTIVE) throw notOpen(productCode, asOf);
            return toEffective(version.get(), asOf);
        }
        if (!product.isOpenOn(asOf)) throw notOpen(productCode, asOf);
        return toEffective(product, asOf);
    }

    @Transactional(readOnly = true)
    public List<ProductDetail> search(ProductStatus status, ProductType productType) {
        return products.search(status, productType).stream().map(this::toDetail).toList();
    }

    @Transactional(readOnly = true)
    public ProductDetail detail(String productCode) {
        return toDetail(require(productCode));
    }

    @Transactional
    public ProductDetail create(CreateProductRequest request) {
        if (products.existsById(request.productCode())) {
            throw ApiException.conflict("PRODUCT_EXISTS", "Product " + request.productCode() + " already exists");
        }
        validateProductShape(request.productType(), request.tenureMonths());
        LocalDate effectiveFrom = request.effectiveFrom() == null ? LocalDate.now() : request.effectiveFrom();
        Product product = Product.builder()
                .productCode(request.productCode())
                .productName(request.productName())
                .productType(request.productType())
                .description(request.description())
                .currency(request.currency() == null ? "INR" : request.currency())
                .interestRate(request.interestRate())
                .minBalance(request.minBalance())
                .minOpeningDeposit(orZero(request.minOpeningDeposit()))
                .maxWithdrawalPerDay(orZero(request.maxWithdrawalPerDay()))
                .freeTxnPerMonth(request.freeTxnPerMonth() == null ? 0 : request.freeTxnPerMonth())
                .tenureMonths(request.tenureMonths())
                .allowsOverdraft(Boolean.TRUE.equals(request.allowsOverdraft()))
                .requiresFunding(request.requiresFunding() == null
                        ? request.productType().isTermBased() : request.requiresFunding())
                .minAge(request.minAge())
                .status(ProductStatus.ACTIVE)
                .effectiveFrom(effectiveFrom)
                .build();
        Product saved = products.saveAndFlush(product);
        history.appendVersion(saved);
        return toDetail(saved);
    }

    @Transactional
    public ProductDetail update(String productCode, UpdateProductRequest request) {
        Product product = requireForUpdate(productCode);
        history.ensureBaseline(product);
        LocalDate effectiveFrom = request.effectiveFrom() == null
                ? LocalDate.now() : request.effectiveFrom();
        history.ensureChronological(productCode, effectiveFrom);

        if (request.productName() != null) product.setProductName(request.productName());
        if (request.description() != null) product.setDescription(request.description());
        if (request.interestRate() != null) product.setInterestRate(request.interestRate());
        if (request.minBalance() != null) product.setMinBalance(request.minBalance());
        if (request.minOpeningDeposit() != null) product.setMinOpeningDeposit(request.minOpeningDeposit());
        if (request.maxWithdrawalPerDay() != null) product.setMaxWithdrawalPerDay(request.maxWithdrawalPerDay());
        if (request.freeTxnPerMonth() != null) product.setFreeTxnPerMonth(request.freeTxnPerMonth());
        if (request.minAge() != null) product.setMinAge(request.minAge());
        product.setEffectiveFrom(effectiveFrom);
        if (request.effectiveTo() != null) product.setEffectiveTo(request.effectiveTo());
        validateDates(product.getEffectiveFrom(), product.getEffectiveTo());

        products.saveAndFlush(product);
        history.appendVersion(product);
        return toDetail(product);
    }

    @Transactional
    public ProductDetail setStatus(String productCode, ProductStatus status) {
        Product product = requireForUpdate(productCode);
        history.ensureBaseline(product);
        LocalDate effectiveFrom = LocalDate.now();
        history.ensureChronological(productCode, effectiveFrom);
        product.setStatus(status);
        product.setEffectiveFrom(effectiveFrom);
        product.setEffectiveTo(null);
        products.saveAndFlush(product);
        history.appendVersion(product);
        return toDetail(product);
    }

    private ProductDetail toDetail(Product product) {
        Optional<ProductVersion> latest = history.latest(product.getProductCode());
        List<ChargeDetail> chargeDetails = charges.findByProductCodeOrderByChargeType(product.getProductCode())
                .stream().map(charge -> new ChargeDetail(charge.getChargeId(), charge.getChargeType(),
                        charge.getAmount(), charge.getFrequency())).toList();
        List<RuleDetail> ruleDetails = rules.findByProductCodeOrderByRuleKey(product.getProductCode())
                .stream().map(rule -> new RuleDetail(rule.getRuleId(), rule.getRuleKey(),
                        rule.getRuleValue(), rule.getDataType(), Boolean.TRUE.equals(rule.getActive())))
                .toList();
        return new ProductDetail(
                product.getProductCode(), product.getProductName(), product.getProductType().name(),
                product.getDescription(), product.getCurrency(), product.getInterestRate(),
                product.getMinBalance(), product.getMinOpeningDeposit(), product.getMaxWithdrawalPerDay(),
                product.getFreeTxnPerMonth(), product.getTenureMonths(),
                Boolean.TRUE.equals(product.getAllowsOverdraft()),
                Boolean.TRUE.equals(product.getRequiresFunding()), product.getMinAge(),
                product.getStatus().name(), product.getEffectiveFrom(), product.getEffectiveTo(),
                product.getCreatedAt(), chargeDetails, ruleDetails,
                latest.map(ProductVersion::getProductVersionId).orElse(null),
                latest.map(ProductVersion::getVersionNumber).orElse(null));
    }

    private EffectiveProduct toEffective(ProductVersion version, LocalDate asOf) {
        return new EffectiveProduct(
                version.getProductCode(), version.getProductName(), version.getProductType().name(),
                version.getCurrency(), version.getInterestRate(), version.getMinBalance(),
                version.getMinOpeningDeposit(), version.getMaxWithdrawalPerDay(),
                version.getFreeTxnPerMonth(), version.getTenureMonths(), history.overdraftLimit(version),
                Boolean.TRUE.equals(version.getAllowsOverdraft()),
                Boolean.TRUE.equals(version.getRequiresFunding()), version.getMinAge(),
                version.getStatus().name(), asOf, version.getProductVersionId(), version.getVersionNumber());
    }

    private EffectiveProduct toEffective(Product product, LocalDate asOf) {
        return new EffectiveProduct(
                product.getProductCode(), product.getProductName(), product.getProductType().name(),
                product.getCurrency(), product.getInterestRate(), product.getMinBalance(),
                product.getMinOpeningDeposit(), product.getMaxWithdrawalPerDay(),
                product.getFreeTxnPerMonth(), product.getTenureMonths(), overdraftLimit(product),
                Boolean.TRUE.equals(product.getAllowsOverdraft()),
                Boolean.TRUE.equals(product.getRequiresFunding()), product.getMinAge(),
                product.getStatus().name(), asOf, null, null);
    }

    private BigDecimal overdraftLimit(Product product) {
        if (!Boolean.TRUE.equals(product.getAllowsOverdraft())) return BigDecimal.ZERO;
        return rules.findByProductCodeAndActiveTrueOrderByRuleKey(product.getProductCode()).stream()
                .filter(rule -> OVERDRAFT_LIMIT_RULE.equals(rule.getRuleKey()))
                .findFirst().map(rule -> new BigDecimal(rule.getRuleValue())).orElse(BigDecimal.ZERO);
    }

    private Product require(String productCode) {
        return products.findById(productCode)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND",
                        "No product with code " + productCode));
    }

    private Product requireForUpdate(String productCode) {
        return products.findByProductCodeForUpdate(productCode)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND",
                        "No product with code " + productCode));
    }

    private ApiException notOpen(String productCode, LocalDate asOf) {
        return ApiException.conflict("PRODUCT_NOT_OPEN",
                "Product " + productCode + " is not open for new business on " + asOf);
    }

    private void validateProductShape(ProductType type, Integer tenureMonths) {
        if (type.isTermBased() && tenureMonths == null) {
            throw ApiException.invalid("TENURE_REQUIRED", type + " products require a tenure");
        }
        if (!type.isTermBased() && tenureMonths != null) {
            throw ApiException.invalid("TENURE_NOT_ALLOWED",
                    "Demand-deposit products must not carry a tenure");
        }
    }

    private void validateDates(LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw ApiException.invalid("INVALID_EFFECTIVE_DATES",
                    "effectiveTo must be on or after effectiveFrom");
        }
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
