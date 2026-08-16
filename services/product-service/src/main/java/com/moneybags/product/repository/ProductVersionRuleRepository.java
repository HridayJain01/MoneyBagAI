package com.moneybags.product.repository;

import com.moneybags.product.entity.ProductVersionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVersionRuleRepository extends JpaRepository<ProductVersionRule, Long> {
    List<ProductVersionRule> findByProductVersionIdOrderByRuleKey(Long productVersionId);
}
