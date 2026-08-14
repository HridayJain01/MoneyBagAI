package com.moneybags.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(name = "rule_key", nullable = false, length = 60)
    private String ruleKey;

    @Column(name = "rule_value", nullable = false, length = 255)
    private String ruleValue;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(nullable = false)
    private Boolean active;
}
