package com.moneybags.security.entity;

import com.moneybags.security.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserSession {
    @Id
    @Column(name = "session_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sessionId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "access_token_hash", nullable = false)
    private String accessTokenHash;
    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;
    @Column(name = "device_type", length = 50)
    private String deviceType;
    @Column(name = "device_name", length = 100)
    private String deviceName;
    @Column(length = 100)
    private String browser;
    @Column(name = "operating_system", length = 100)
    private String operatingSystem;
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;
    @Column(name = "last_activity_time", nullable = false)
    private LocalDateTime lastActivityTime;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "logout_time")
    private LocalDateTime logoutTime;
    @Enumerated(EnumType.STRING) @Column(name = "session_status", nullable = false, length = 20)
    private SessionStatus sessionStatus;
}
