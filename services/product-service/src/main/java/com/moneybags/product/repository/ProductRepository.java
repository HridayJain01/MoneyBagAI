package com.moneybags.product.repository;

import com.moneybags.product.entity.Product;
import com.moneybags.product.entity.ProductStatus;
import com.moneybags.product.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("""
            SELECT p FROM Product p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:productType IS NULL OR p.productType = :productType)
            ORDER BY p.productCode
            """)
    List<Product> search(@Param("status") ProductStatus status,
                         @Param("productType") ProductType productType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productCode = :productCode")
    Optional<Product> findByProductCodeForUpdate(@Param("productCode") String productCode);
}
