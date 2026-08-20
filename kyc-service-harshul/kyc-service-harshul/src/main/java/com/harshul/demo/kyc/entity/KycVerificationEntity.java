package com.harshul.demo.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "KYC_VERIFICATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationEntity extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SESSION_ID", nullable = false, unique = true)
    private KycSessionEntity session;

    @Column(name = "RESULT", length = 50, nullable = false)
    private String result;

    @Column(name = "REVIEWER", length = 50, nullable = false)
    private String reviewerId;
}