package com.moneybags.customer.entity;

import com.moneybags.customer.enums.DocumentVerifyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    @Column(name = "verified_by")
    private Long verifiedBy;
}
