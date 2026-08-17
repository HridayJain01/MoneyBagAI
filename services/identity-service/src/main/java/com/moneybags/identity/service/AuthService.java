package com.moneybags.identity.service;

import com.moneybags.identity.api.ApiModels.LoginRequest;
import com.moneybags.identity.api.ApiModels.LoginResponse;
import com.moneybags.identity.config.IdentityConfig.IdentityProperties;
import com.moneybags.identity.entity.LoginAudit;
import com.moneybags.identity.entity.Role;
import com.moneybags.identity.entity.User;
import com.moneybags.identity.entity.UserStatus;
import com.moneybags.identity.repository.LoginAuditRepository;
import com.moneybags.identity.repository.UserRepository;
import com.moneybags.identity.support.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final LoginAuditRepository audits;
    private final PasswordEncoder passwordEncoder;
    private final IdentityProperties properties;
    private final JwtTokenService tokens;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        User user = users.findByUsername(request.username()).orElse(null);

        if (user == null) {
            audit(null, request.username(), "LOGIN_FAILED", ip, userAgent, "UNKNOWN_USER", "FAILURE");
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Username or password is incorrect");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            audit(user.getUserId(), user.getUsername(), "LOGIN_FAILED", ip, userAgent, "USER_DISABLED", "FAILURE");
            throw ApiException.forbidden("USER_DISABLED", "This user is disabled");
        }
        if (user.isLocked()) {
            audit(user.getUserId(), user.getUsername(), "LOGIN_FAILED", ip, userAgent, "USER_LOCKED", "FAILURE");
            throw ApiException.forbidden("USER_LOCKED", "This user is locked until " + user.getLockedUntil());
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailure(user, ip, userAgent);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Username or password is incorrect");
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setLastLoginAt(Instant.now());
        users.save(user);

        List<String> roles = user.getRoles().stream().map(Role::getRoleName).sorted().toList();
        List<String> permissions = user.permissionCodes().stream().sorted().toList();
        JwtTokenService.IssuedToken issued = tokens.issue(user, roles, permissions);
        audit(user.getUserId(), user.getUsername(), "LOGIN_SUCCESS", ip, userAgent, null, "SUCCESS");

        return new LoginResponse(
                issued.value(), "Bearer", issued.expiresAt(), user.getUserId(), user.getUsername(),
                user.getFullName(), user.getEmployeeId(), user.getBranchCode(), roles, permissions);
    }

    @Transactional
    public void recordLogout(Long userId, String ip, String userAgent) {
        User user = users.findById(userId).orElse(null);
        if (user != null) {
            audit(userId, user.getUsername(), "LOGOUT", ip, userAgent, null, "SUCCESS");
        }
    }

    private void registerFailure(User user, String ip, String userAgent) {
        user.setFailedAttempts(user.getFailedAttempts() + 1);
        String reason = "BAD_PASSWORD";
        if (user.getFailedAttempts() >= properties.getMaxFailedAttempts()) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(Instant.now().plus(properties.getLockDurationMinutes(), ChronoUnit.MINUTES));
            reason = "LOCKED_AFTER_" + user.getFailedAttempts() + "_FAILURES";
            log.warn("User {} locked after {} failed attempts", user.getUsername(), user.getFailedAttempts());
        }
        users.save(user);
        audit(user.getUserId(), user.getUsername(), "LOGIN_FAILED", ip, userAgent, reason, "FAILURE");
    }

    private void audit(Long userId, String username, String eventType, String ip,
                       String userAgent, String failureReason, String outcome) {
        audits.save(LoginAudit.builder()
                .userId(userId)
                .username(username)
                .eventType(eventType)
                .eventTime(Instant.now())
                .ipAddress(ip)
                .deviceInfo(truncate(userAgent))
                .failureReason(failureReason)
                .outcome(outcome)
                .build());
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
