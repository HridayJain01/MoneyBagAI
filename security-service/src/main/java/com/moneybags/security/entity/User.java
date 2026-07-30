package com.moneybags.security.entity;

import com.moneybags.security.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false, length = 80)
    private String username;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;
    @Column(length = 20)
    private String mobile;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private UserStatus status;
    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts;
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        failedAttempts = failedAttempts == null ? 0 : failedAttempts;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
