package com.moneybags.product.repository;

import com.moneybags.product.entity.ProductCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductChargeRepository extends JpaRepository<ProductCharge, Long> {
    List<ProductCharge> findByProductCodeOrderByChargeType(String productCode);

    void deleteByProductCode(String productCode);
}
