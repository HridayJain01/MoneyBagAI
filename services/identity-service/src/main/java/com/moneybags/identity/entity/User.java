package com.moneybags.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @SequenceGenerator(name = "identity_user_sequence", sequenceName = "identity_user_seq", initialValue = 2000, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "identity_user_sequence")
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 80, unique = true)
    private String username;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 20)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** Logical reference to branch-employee-service. Denormalised for one-call session resolve. */
    @Column(name = "employee_id", length = 64)
    private String employeeId;

    /** Logical reference to branch-employee-service. Same value as X-Branch-Code and X-Branch-Id. */
    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Flattened permission codes across every assigned role. */
    public Set<String> permissionCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (Role role : roles) {
            for (Permission permission : role.getPermissions()) {
                codes.add(permission.getPermissionCode());
            }
        }
        return codes;
    }

    public boolean isLocked() {
        return status == UserStatus.LOCKED
                && lockedUntil != null
                && lockedUntil.isAfter(Instant.now());
    }
}
