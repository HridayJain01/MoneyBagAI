package com.moneybags.customer.entity;

import com.moneybags.customer.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Customer {
    @Id @Column(name = "cif_no", length = 30)
    private String cifNo;
    @Column(name = "user_id", unique = true)
    private Long userId;
    @Column(name = "relationship_manager_emp_id")
    private Long relationshipManagerEmpId;
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;
    @Column(name = "last_name", length = 80)
    private String lastName;
    @Column(nullable = false)
    private LocalDate dob;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Gender gender;
    @Column(name = "pan_no", nullable = false, unique = true, length = 10)
    private String panNo;
    @Column(nullable = false, length = 20)
    private String mobile;
    @Column(length = 150)
    private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private CustomerStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;
    @Column(name = "kyc_failure_count", nullable = false)
    private Integer kycFailureCount;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; kycFailureCount = kycFailureCount == null ? 0 : kycFailureCount; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public void setCifNo(String cifNo) { this.cifNo = cifNo; }
}
