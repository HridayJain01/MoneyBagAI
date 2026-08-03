package com.moneybags.customer.entity;

import com.moneybags.customer.enums.DocumentVerifyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KycDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cif_no", nullable = false)
    private Customer customer;
    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;
    @Column(name = "doc_number", nullable = false, length = 80)
    private String docNumber;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    @Enumerated(EnumType.STRING) @Column(name = "verify_status", nullable = false, length = 20)
    private DocumentVerifyStatus verifyStatus;
    @Column(name = "assigned_to_emp_id")
    private Long assignedToEmpId;
    @Column(name = "verified_by_emp_id")
    private Long verifiedByEmpId;
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    @PrePersist void onCreate() { submittedAt = submittedAt == null ? LocalDateTime.now() : submittedAt; }
}
