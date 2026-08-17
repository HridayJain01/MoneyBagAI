package com.harshul.demo.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyc_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationEntity extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private KycSessionEntity session;

    @Column(name = "result", length = 500, nullable = false)
    private String result;

    @Column(name = "reviewer_id", length = 50, nullable = false)
    private String reviewerId;
}
