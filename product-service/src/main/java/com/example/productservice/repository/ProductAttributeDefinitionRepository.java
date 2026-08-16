package com.example.productservice.repository;

import com.example.productservice.entity.ProductAttributeDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductAttributeDefinitionRepository extends JpaRepository<ProductAttributeDefinition, Long> {

    Optional<ProductAttributeDefinition> findByAttributeCode(String attributeCode);

    @Query("select coalesce(max(pad.id), 0) from ProductAttributeDefinition pad")
    Long findMaxId();
}
