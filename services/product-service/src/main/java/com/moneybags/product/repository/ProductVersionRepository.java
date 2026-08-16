package com.moneybags.product.repository;

import com.moneybags.product.entity.ProductVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductVersionRepository extends JpaRepository<ProductVersion, Long> {
    List<ProductVersion> findByProductCodeOrderByVersionNumberDesc(String productCode);

    Optional<ProductVersion> findFirstByProductCodeOrderByVersionNumberDesc(String productCode);

    Optional<ProductVersion> findByProductCodeAndVersionNumber(String productCode, Integer versionNumber);

    List<ProductVersion> findByProductCodeAndEffectiveFromLessThanEqualOrderByVersionNumberDesc(
            String productCode, LocalDate businessDate);
}
