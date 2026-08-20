package com.moneybags.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.moneybags.customer.enums.DocumentVerifyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_kyc_documents")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KycDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cif_no", nullable = false)
    private Customer customer;
    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;
    @JsonIgnore
    @Column(name = "doc_number", nullable = false, length = 80)
    private String docNumber;
    @Column(name = "document_number_hash", nullable = false, length = 64)
    private String documentNumberHash;
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
    @Column(name = "expiry_alerted_at")
    private LocalDateTime expiryAlertedAt;
    @PrePersist void onCreate() { submittedAt = submittedAt == null ? LocalDateTime.now() : submittedAt; }
}
