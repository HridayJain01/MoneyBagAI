package com.moneybags.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_version_rules")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVersionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_rule_id")
    private Long versionRuleId;

    @Column(name = "product_version_id", nullable = false)
    private Long productVersionId;

    @Column(name = "rule_key", nullable = false, length = 60)
    private String ruleKey;

    @Column(name = "rule_value", nullable = false, length = 255)
    private String ruleValue;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(nullable = false)
    private Boolean active;
}
