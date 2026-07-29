package com.moneybags.customer.entity;

import com.moneybags.customer.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cif_no")
    private Long cifNo;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private LocalDate dob;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Gender gender;
    @Column(name = "pan_no", nullable = false, unique = true, length = 10)
    private String panNo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private CustomerStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;
}
