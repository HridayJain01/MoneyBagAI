package com.example.productservice.repository;

import com.example.productservice.entity.ProductVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductVersionRepository extends JpaRepository<ProductVersion, Long> {

    Optional<ProductVersion> findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(Long productId, String status);

    List<ProductVersion> findByProduct_IdOrderByVersionNumberDesc(Long productId);

    Optional<ProductVersion> findFirstByProduct_IdOrderByVersionNumberDesc(Long productId);

    @Query("select coalesce(max(pv.id), 0) from ProductVersion pv")
    Long findMaxId();
}
