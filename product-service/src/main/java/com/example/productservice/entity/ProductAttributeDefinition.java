package com.example.productservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_attribute_definitions")
public class ProductAttributeDefinition {

    @Id
    private Long id;

    @Column(name = "attribute_code", nullable = false, unique = true, length = 50)
    private String attributeCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "unit", length = 20)
    private String unit;
}
