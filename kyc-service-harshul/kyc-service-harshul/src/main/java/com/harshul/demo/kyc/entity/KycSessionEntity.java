package com.harshul.demo.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "KYC_SESSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KycSessionEntity extends AbstractEntity {

    @Column(name = "EXTERNAL_USER_ID", length = 100, nullable = false)
    private String externalUserId;

    @Column(name = "PURPOSE", length = 100)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "DOCUMENT_TYPE", length = 30, nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 40, nullable = false)
    private KycSessionStatus status;

    @PrePersist
    @Override
    protected void init() {
        super.init();
        if (status == null) {
            status = KycSessionStatus.CREATED;
        }
    }
}