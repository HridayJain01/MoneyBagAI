package com.moneybags.product.repository;

import com.moneybags.product.entity.ProductVersionCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVersionChargeRepository extends JpaRepository<ProductVersionCharge, Long> {
    List<ProductVersionCharge> findByProductVersionIdOrderByChargeType(Long productVersionId);
}
