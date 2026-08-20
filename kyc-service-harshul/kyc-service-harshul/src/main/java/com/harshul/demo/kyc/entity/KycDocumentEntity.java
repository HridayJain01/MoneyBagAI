package com.harshul.demo.kyc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "KYC_DOCUMENT")
@Setter
@Getter
@NoArgsConstructor
public class KycDocumentEntity extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SESSION_ID", nullable = false)
    private KycSessionEntity session;

    @Enumerated(EnumType.STRING)
    @Column(name = "DOCUMENT_TYPE", length = 30, nullable = false)
    private DocumentType documentType;

    @Column(name = "ORIGINAL_FILE_NAME", length = 255, nullable = false)
    private String originalFileName;

    @Column(name = "CONTENT_TYPE", length = 100)
    private String contentType;

    @Column(name = "FILE_SIZE", nullable = false)
    private long size;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private byte[] content;
}
