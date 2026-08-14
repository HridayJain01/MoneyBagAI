package com.moneybags.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.moneybags.customer.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_addresses")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerAddress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cif_no", nullable = false)
    private Customer customer;
    @Enumerated(EnumType.STRING) @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;
    @Column(nullable = false, length = 150)
    private String line1;
    @Column(nullable = false, length = 80)
    private String city;
    @Column(nullable = false, length = 80)
    private String state;
    @Column(nullable = false, length = 10)
    private String pincode;
    @Column(nullable = false, length = 80)
    private String country;
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;
}
