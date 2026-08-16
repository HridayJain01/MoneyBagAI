package com.moneybags.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_version_charges")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVersionCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_charge_id")
    private Long versionChargeId;

    @Column(name = "product_version_id", nullable = false)
    private Long productVersionId;

    @Column(name = "charge_type", nullable = false, length = 50)
    private String chargeType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    private String frequency;
}
