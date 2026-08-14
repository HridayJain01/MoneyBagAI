package com.moneybags.product.repository;

import com.moneybags.product.entity.ProductRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRuleRepository extends JpaRepository<ProductRule, Long> {
    List<ProductRule> findByProductCodeAndActiveTrueOrderByRuleKey(String productCode);

    List<ProductRule> findByProductCodeOrderByRuleKey(String productCode);
}
