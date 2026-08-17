package com.moneybags.customer.entity;

import com.moneybags.customer.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;

@Entity
@Table(name = "customers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Customer {
    @Id @Column(name = "cif_no", length = 30)
    private String cifNo;
    @Column(name = "user_id", unique = true)
    private Long userId;
    @Column(name = "relationship_manager_emp_id")
    private Long relationshipManagerEmpId;
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;
    @Column(name = "last_name", length = 80)
    private String lastName;
    @Column(nullable = false)
    private LocalDate dob;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Gender gender;
    @Column(name = "pan_no", nullable = false, unique = true, length = 10)
    private String panNo;
    @Column(nullable = false, length = 20)
    private String mobile;
    @Column(length = 150)
    private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private CustomerStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "kyc_status", nullable = false, length = 20)
    private KycStatus kycStatus;
    @Enumerated(EnumType.STRING) @Column(name = "risk_classification", nullable = false, length = 20)
    private RiskClassification riskClassification;
    @Enumerated(EnumType.STRING) @Column(name = "preferred_communication_channel", nullable = false, length = 20)
    private CommunicationChannel preferredCommunicationChannel;
    @Column(name = "email_notifications_enabled", nullable = false)
    private Boolean emailNotificationsEnabled;
    @Column(name = "sms_notifications_enabled", nullable = false)
    private Boolean smsNotificationsEnabled;
    @Column(name = "push_notifications_enabled", nullable = false)
    private Boolean pushNotificationsEnabled;
    @Column(name = "kyc_failure_count", nullable = false)
    private Integer kycFailureCount;
    @Column(name = "external_kyc_session_id", length = 36)
    private String externalKycSessionId;
    @Column(name = "external_kyc_decision", length = 20)
    private String externalKycDecision;
    @Column(name = "external_kyc_decided_at")
    private Instant externalKycDecidedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        var now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
        kycFailureCount = kycFailureCount == null ? 0 : kycFailureCount;
        riskClassification = riskClassification == null ? RiskClassification.LOW : riskClassification;
        preferredCommunicationChannel = preferredCommunicationChannel == null
                ? CommunicationChannel.EMAIL
                : preferredCommunicationChannel;
        emailNotificationsEnabled = emailNotificationsEnabled == null || emailNotificationsEnabled;
        smsNotificationsEnabled = smsNotificationsEnabled == null || smsNotificationsEnabled;
        pushNotificationsEnabled = pushNotificationsEnabled != null && pushNotificationsEnabled;
    }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public void setCifNo(String cifNo) { this.cifNo = cifNo; }
}
