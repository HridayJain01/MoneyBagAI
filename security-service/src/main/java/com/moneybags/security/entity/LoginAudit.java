package com.moneybags.security.entity;

import com.moneybags.security.enums.LoginEventType;
import com.moneybags.security.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_audit")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LoginAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false, length = 80)
    private String username;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 30)
    private LoginEventType eventType;
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    @Column(name = "device_info", length = 255)
    private String deviceInfo;
    @Column(name = "failure_reason", length = 255)
    private String failureReason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RecordStatus status;
}
