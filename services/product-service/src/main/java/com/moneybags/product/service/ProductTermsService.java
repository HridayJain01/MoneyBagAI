package com.moneybags.product.service;

import com.moneybags.product.dto.*;
import com.moneybags.product.entity.*;
import com.moneybags.product.repository.*;
import com.moneybags.product.support.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductTermsService {

    private final ProductRepository products;
    private final ProductChargeRepository charges;
    private final ProductRuleRepository rules;
    private final ProductHistoryService history;

    @Transactional
    public List<ChargeDetail> replaceCharges(String productCode, List<ChargeRequest> requests) {
        Product product = prepareVersionedChange(productCode);
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
        charges.flush();
        history.appendVersion(product);
        return saved.stream().map(this::toDetail).toList();
    }

    @Transactional(readOnly = true)
    public List<ChargeDetail> charges(String productCode) {
        require(productCode);
        return charges.findByProductCodeOrderByChargeType(productCode).stream()
                .map(this::toDetail).toList();
    }

    @Transactional(readOnly = true)
    public List<RuleDetail> rules(String productCode) {
        require(productCode);
        return rules.findByProductCodeOrderByRuleKey(productCode).stream()
                .map(this::toDetail).toList();
    }

    @Transactional
    public RuleDetail addRule(String productCode, RuleRequest request) {
        Product product = prepareVersionedChange(productCode);
        ProductRule saved = rules.saveAndFlush(ProductRule.builder()
                .productCode(productCode)
                .ruleKey(request.ruleKey())
                .ruleValue(request.ruleValue())
                .dataType(request.dataType())
                .active(true)
                .build());
        history.appendVersion(product);
        return toDetail(saved);
    }

    @Transactional
    public void retireRule(String productCode, Long ruleId) {
        Product product = requireForUpdate(productCode);
        history.ensureBaseline(product);
        ProductRule rule = rules.findById(ruleId)
                .orElseThrow(() -> ApiException.notFound("RULE_NOT_FOUND", "No such rule"));
        if (!rule.getProductCode().equals(productCode)) {
            throw ApiException.invalid("RULE_PRODUCT_MISMATCH", "Rule does not belong to this product");
        }
        prepareEffectiveToday(product);
        rule.setActive(false);
        rules.saveAndFlush(rule);
        history.appendVersion(product);
    }

    private Product prepareVersionedChange(String productCode) {
        Product product = requireForUpdate(productCode);
        history.ensureBaseline(product);
        prepareEffectiveToday(product);
        return product;
    }

    private void prepareEffectiveToday(Product product) {
        LocalDate effectiveFrom = LocalDate.now();
        history.ensureChronological(product.getProductCode(), effectiveFrom);
        product.setEffectiveFrom(effectiveFrom);
        product.setEffectiveTo(null);
        products.saveAndFlush(product);
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

    private ChargeDetail toDetail(ProductCharge charge) {
        return new ChargeDetail(charge.getChargeId(), charge.getChargeType(),
                charge.getAmount(), charge.getFrequency());
    }

    private RuleDetail toDetail(ProductRule rule) {
        return new RuleDetail(rule.getRuleId(), rule.getRuleKey(), rule.getRuleValue(),
                rule.getDataType(), Boolean.TRUE.equals(rule.getActive()));
    }
}
