package com.harshul.demo.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyc_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KycSessionEntity extends AbstractEntity {

    @Column(name = "cif_no", length = 30, nullable = false)
    private String cifNo;

    @Column(name = "purpose", length = 100)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 30, nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
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
