package com.moneybags.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "kyc_rejection_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KycRejectionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long rejectionId;
    @Column(name = "cif_no", nullable = false, length = 30) private String cifNo;
    @Column(name = "doc_id", nullable = false) private Long docId;
    @Column(name = "failure_reason", nullable = false, length = 500) private String failureReason;
    @Column(name = "rejected_by_emp_id") private Long rejectedByEmpId;
    @Column(name = "attempt_number", nullable = false) private Integer attemptNumber;
    @Column(name = "rejected_at", nullable = false) private LocalDateTime rejectedAt;
    @PrePersist void onCreate() { rejectedAt = rejectedAt == null ? LocalDateTime.now() : rejectedAt; }
}
