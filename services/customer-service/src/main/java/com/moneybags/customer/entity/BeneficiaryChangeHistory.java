package com.moneybags.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "beneficiary_change_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BeneficiaryChangeHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long historyId;
    @Column(name = "beneficiary_id", nullable = false) private Long beneficiaryId;
    @Column(name = "cif_no", nullable = false, length = 30) private String cifNo;
    @Column(name = "change_type", nullable = false, length = 40) private String changeType;
    @Column(name = "changed_at", nullable = false) private LocalDateTime changedAt;
    @PrePersist void onCreate() { changedAt = changedAt == null ? LocalDateTime.now() : changedAt; }
}
