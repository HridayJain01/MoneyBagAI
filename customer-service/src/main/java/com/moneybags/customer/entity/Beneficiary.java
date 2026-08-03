package com.moneybags.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "beneficiaries", uniqueConstraints = @UniqueConstraint(columnNames = {"cif_no", "beneficiary_account_no", "beneficiary_ifsc"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Beneficiary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "beneficiary_id") private Long beneficiaryId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cif_no") private Customer customer;
    @Column(name = "beneficiary_name", nullable = false, length = 150) private String beneficiaryName;
    @Column(name = "beneficiary_account_no", nullable = false, length = 30) private String beneficiaryAccountNo;
    @Column(name = "beneficiary_bank_name", length = 150) private String beneficiaryBankName;
    @Column(name = "beneficiary_ifsc", nullable = false, length = 20) private String beneficiaryIfsc;
    @Column(name = "beneficiary_nickname", length = 80) private String beneficiaryNickname;
    @Column(name = "beneficiary_type", nullable = false, length = 30) private String beneficiaryType;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "added_at", nullable = false) private LocalDateTime addedAt;
    @Column(name = "activated_at") private LocalDateTime activatedAt;
    @PrePersist void onCreate() { addedAt = addedAt == null ? LocalDateTime.now() : addedAt; }
}
