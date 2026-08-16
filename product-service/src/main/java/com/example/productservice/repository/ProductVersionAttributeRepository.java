package com.example.productservice.repository;

import com.example.productservice.entity.ProductVersionAttribute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductVersionAttributeRepository extends JpaRepository<ProductVersionAttribute, Long> {

    List<ProductVersionAttribute> findByProductVersion_Id(Long productVersionId);

    @Query("select coalesce(max(pva.id), 0) from ProductVersionAttribute pva")
    Long findMaxId();
}
