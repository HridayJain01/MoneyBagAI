package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductCode(String productCode);

    boolean existsByProductCode(String productCode);

    List<Product> findByProductCategory(String productCategory);

    List<Product> findByProductType(String productType);

    @Query("select coalesce(max(p.id), 0) from Product p")
    Long findMaxId();
}
