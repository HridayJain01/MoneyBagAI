package com.moneybags.product.service;

import com.moneybags.product.api.ApiModels.*;
import com.moneybags.product.entity.*;
import com.moneybags.product.repository.*;
import com.moneybags.product.support.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String OVERDRAFT_LIMIT_RULE = "OVERDRAFT_LIMIT";

    private final ProductRepository products;
    private final ProductChargeRepository charges;
    private final ProductRuleRepository rules;

    /**
     * Resolves the terms account-service snapshots at opening.
     *
     * <p>Rejects a product that is not open for new business on the business date --
     * account-service must not be able to open an account against a retired product.
     */
    @Transactional(readOnly = true)
    public EffectiveProduct effective(String productCode, LocalDate businessDate) {
        Product product = require(productCode);
        LocalDate asOf = businessDate == null ? LocalDate.now() : businessDate;
        if (!product.isOpenOn(asOf)) {
            throw ApiException.conflict("PRODUCT_NOT_OPEN",
                    "Product " + productCode + " is not open for new business on " + asOf);
        }
        return new EffectiveProduct(
                product.getProductCode(),
                product.getProductName(),
                product.getProductType().name(),
                product.getCurrency(),
                product.getInterestRate(),
                product.getMinBalance(),
                product.getMinOpeningDeposit(),
                product.getMaxWithdrawalPerDay(),
                product.getFreeTxnPerMonth(),
                product.getTenureMonths(),
                overdraftLimit(product),
                Boolean.TRUE.equals(product.getAllowsOverdraft()),
                Boolean.TRUE.equals(product.getRequiresFunding()),
                product.getMinAge(),
                product.getStatus().name(),
                asOf);
    }

    /** Overdraft capacity lives as a rule so it can change without a schema migration. */
    private BigDecimal overdraftLimit(Product product) {
        if (!Boolean.TRUE.equals(product.getAllowsOverdraft())) {
            return BigDecimal.ZERO;
        }
        return rules.findByProductCodeAndActiveTrueOrderByRuleKey(product.getProductCode()).stream()
                .filter(rule -> OVERDRAFT_LIMIT_RULE.equals(rule.getRuleKey()))
                .findFirst()
                .map(rule -> new BigDecimal(rule.getRuleValue()))
                .orElse(BigDecimal.ZERO);
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
        boolean termBased = request.productType().isTermBased();
        if (termBased && request.tenureMonths() == null) {
            throw ApiException.invalid("TENURE_REQUIRED",
                    request.productType() + " products require a tenure");
        }
        if (!termBased && request.tenureMonths() != null) {
            throw ApiException.invalid("TENURE_NOT_ALLOWED",
                    "Demand-deposit products must not carry a tenure");
        }

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
                .requiresFunding(request.requiresFunding() == null ? termBased : request.requiresFunding())
                .minAge(request.minAge())
                .status(ProductStatus.ACTIVE)
                .effectiveFrom(request.effectiveFrom() == null ? LocalDate.now() : request.effectiveFrom())
                .build();
        return toDetail(products.save(product));
    }

    @Transactional
    public ProductDetail update(String productCode, UpdateProductRequest request) {
        Product product = require(productCode);
        if (request.productName() != null) {
            product.setProductName(request.productName());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.interestRate() != null) {
            product.setInterestRate(request.interestRate());
        }
        if (request.minBalance() != null) {
            product.setMinBalance(request.minBalance());
        }
        if (request.minOpeningDeposit() != null) {
            product.setMinOpeningDeposit(request.minOpeningDeposit());
        }
        if (request.maxWithdrawalPerDay() != null) {
            product.setMaxWithdrawalPerDay(request.maxWithdrawalPerDay());
        }
        if (request.freeTxnPerMonth() != null) {
            product.setFreeTxnPerMonth(request.freeTxnPerMonth());
        }
        if (request.minAge() != null) {
            product.setMinAge(request.minAge());
        }
        if (request.effectiveTo() != null) {
            product.setEffectiveTo(request.effectiveTo());
        }
        return toDetail(products.save(product));
    }

    /**
     * Deactivation stops new sales only. Accounts already opened against the product
     * keep their snapshotted terms and continue to operate.
     */
    @Transactional
    public ProductDetail setStatus(String productCode, ProductStatus status) {
        Product product = require(productCode);
        product.setStatus(status);
        return toDetail(products.save(product));
    }

    @Transactional
    public List<ChargeDetail> replaceCharges(String productCode, List<ChargeRequest> requests) {
        require(productCode);
        charges.deleteByProductCode(productCode);
        charges.flush();
        List<ProductCharge> saved = charges.saveAll(requests.stream()
                .map(request -> ProductCharge.builder()
                        .productCode(productCode)
                        .chargeType(request.chargeType())
                        .amount(request.amount())
                        .frequency(request.frequency())
                        .build())
                .toList());
        return saved.stream().map(this::toChargeDetail).toList();
    }

    @Transactional(readOnly = true)
    public List<ChargeDetail> charges(String productCode) {
        require(productCode);
        return charges.findByProductCodeOrderByChargeType(productCode).stream()
                .map(this::toChargeDetail).toList();
    }

    @Transactional(readOnly = true)
    public List<RuleDetail> rules(String productCode) {
        require(productCode);
        return rules.findByProductCodeOrderByRuleKey(productCode).stream()
                .map(this::toRuleDetail).toList();
    }

    @Transactional
    public RuleDetail addRule(String productCode, RuleRequest request) {
        require(productCode);
        ProductRule rule = ProductRule.builder()
                .productCode(productCode)
                .ruleKey(request.ruleKey())
                .ruleValue(request.ruleValue())
                .dataType(request.dataType())
                .active(true)
                .build();
        return toRuleDetail(rules.save(rule));
    }

    @Transactional
    public void retireRule(String productCode, Long ruleId) {
        ProductRule rule = rules.findById(ruleId)
                .orElseThrow(() -> ApiException.notFound("RULE_NOT_FOUND", "No such rule"));
        if (!rule.getProductCode().equals(productCode)) {
            throw ApiException.invalid("RULE_PRODUCT_MISMATCH", "Rule does not belong to this product");
        }
        // Retired rather than deleted: an account opened under it may still be audited.
        rule.setActive(false);
        rules.save(rule);
    }

    private Product require(String productCode) {
        return products.findById(productCode)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND",
                        "No product with code " + productCode));
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ProductDetail toDetail(Product product) {
        return new ProductDetail(
                product.getProductCode(), product.getProductName(), product.getProductType().name(),
                product.getDescription(), product.getCurrency(), product.getInterestRate(),
                product.getMinBalance(), product.getMinOpeningDeposit(), product.getMaxWithdrawalPerDay(),
                product.getFreeTxnPerMonth(), product.getTenureMonths(),
                Boolean.TRUE.equals(product.getAllowsOverdraft()),
                Boolean.TRUE.equals(product.getRequiresFunding()),
                product.getMinAge(), product.getStatus().name(),
                product.getEffectiveFrom(), product.getEffectiveTo(), product.getCreatedAt(),
                charges.findByProductCodeOrderByChargeType(product.getProductCode()).stream()
                        .map(this::toChargeDetail).toList(),
                rules.findByProductCodeOrderByRuleKey(product.getProductCode()).stream()
                        .map(this::toRuleDetail).toList());
    }

    private ChargeDetail toChargeDetail(ProductCharge charge) {
        return new ChargeDetail(charge.getChargeId(), charge.getChargeType(),
                charge.getAmount(), charge.getFrequency());
    }

    private RuleDetail toRuleDetail(ProductRule rule) {
        return new RuleDetail(rule.getRuleId(), rule.getRuleKey(), rule.getRuleValue(),
                rule.getDataType(), Boolean.TRUE.equals(rule.getActive()));
    }
}
