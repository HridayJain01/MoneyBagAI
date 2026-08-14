package com.moneybags.identity.service;

import com.moneybags.identity.api.ApiModels.*;
import com.moneybags.identity.config.IdentityConfig.IdentityProperties;
import com.moneybags.identity.entity.*;
import com.moneybags.identity.entity.SessionStatus;
import com.moneybags.identity.entity.UserStatus;
import com.moneybags.identity.repository.*;
import com.moneybags.identity.support.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final SessionRepository sessions;
    private final LoginAuditRepository audits;
    private final PasswordEncoder passwordEncoder;
    private final IdentityProperties properties;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String userAgent) {
        User user = users.findByUsername(request.username())
                .orElse(null);

        if (user == null) {
            // Audited under the submitted username so credential-stuffing against
            // non-existent accounts is still visible.
            audit(null, request.username(), "LOGIN_FAILED", ip, userAgent, "UNKNOWN_USER", "FAILURE");
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Username or password is incorrect");
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            audit(user.getUserId(), user.getUsername(), "LOGIN_FAILED", ip, userAgent, "USER_DISABLED", "FAILURE");
            throw ApiException.forbidden("USER_DISABLED", "This user is disabled");
        }

        if (user.isLocked()) {
            audit(user.getUserId(), user.getUsername(), "LOGIN_FAILED", ip, userAgent, "USER_LOCKED", "FAILURE");
            throw ApiException.forbidden("USER_LOCKED",
                    "This user is locked until " + user.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailure(user, ip, userAgent);
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Username or password is incorrect");
        }

        // A lock that has aged out is cleared on the next good password.
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setLastLoginAt(Instant.now());
        users.save(user);

        UserSession session = openSession(user, ip, userAgent);
        audit(user.getUserId(), user.getUsername(), "LOGIN_SUCCESS", ip, userAgent, null, "SUCCESS");

        return new LoginResponse(
                session.getSessionId(),
                session.getExpiresAt(),
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmployeeId(),
                user.getBranchCode(),
                user.getRoles().stream().map(Role::getRoleName).sorted().toList(),
                user.permissionCodes().stream().sorted().toList());
    }

    private void registerFailure(User user, String ip, String userAgent) {
        user.setFailedAttempts(user.getFailedAttempts() + 1);
        String reason = "BAD_PASSWORD";
        if (user.getFailedAttempts() >= properties.getMaxFailedAttempts()) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(Instant.now().plus(properties.getLockDurationMinutes(), ChronoUnit.MINUTES));
            // Locking the account is pointless if existing sessions keep working.
            sessions.revokeAllForUser(user.getUserId(), SessionStatus.REVOKED, SessionStatus.ACTIVE, Instant.now());
            reason = "LOCKED_AFTER_" + user.getFailedAttempts() + "_FAILURES";
            log.warn("User {} locked after {} failed attempts", user.getUsername(), user.getFailedAttempts());
        }
        users.save(user);
        audit(user.getUserId(), user.getUsername(), "LOGIN_FAILED", ip, userAgent, reason, "FAILURE");
    }

    private UserSession openSession(User user, String ip, String userAgent) {
        Instant now = Instant.now();
        UserSession session = UserSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(user.getUserId())
                .status(SessionStatus.ACTIVE)
                .issuedAt(now)
                .lastActivityAt(now)
                .expiresAt(now.plus(properties.getSessionTtlMinutes(), ChronoUnit.MINUTES))
                .ipAddress(ip)
                .userAgent(truncate(userAgent))
                .build();
        return sessions.save(session);
    }

    /**
     * Called by the gateway on every authenticated request. Kept deliberately cheap:
     * one session read, one user read, no writes on the hot path beyond a
     * last-activity touch.
     */
    @Transactional
    public SessionPrincipal resolve(String sessionId) {
        UserSession session = sessions.findById(sessionId)
                .orElseThrow(() -> ApiException.unauthorized("SESSION_INVALID", "Unknown session"));

        if (session.getStatus() == SessionStatus.REVOKED) {
            throw ApiException.unauthorized("SESSION_REVOKED", "Session has been revoked");
        }
        if (!session.isUsable()) {
            if (session.getStatus() == SessionStatus.ACTIVE) {
                session.setStatus(SessionStatus.EXPIRED);
                sessions.save(session);
            }
            throw ApiException.unauthorized("SESSION_EXPIRED", "Session has expired");
        }

        User user = users.findById(session.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("SESSION_INVALID", "Session user no longer exists"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.forbidden("USER_NOT_ACTIVE", "User is " + user.getStatus());
        }

        session.setLastActivityAt(Instant.now());
        sessions.save(session);

        return new SessionPrincipal(
                user.getUserId(),
                user.getUsername(),
                user.getEmployeeId(),
                user.getBranchCode(),
                user.permissionCodes().stream().sorted().toList(),
                user.getStatus().name(),
                session.getExpiresAt());
    }

    @Transactional
    public void logout(String sessionId) {
        sessions.findById(sessionId).ifPresent(session -> {
            session.setStatus(SessionStatus.REVOKED);
            session.setRevokedAt(Instant.now());
            sessions.save(session);
            audit(session.getUserId(), String.valueOf(session.getUserId()),
                    "LOGOUT", session.getIpAddress(), null, null, "SUCCESS");
        });
    }

    @Transactional
    public int logoutAll(String sessionId) {
        UserSession session = sessions.findById(sessionId)
                .orElseThrow(() -> ApiException.unauthorized("SESSION_INVALID", "Unknown session"));
        return sessions.revokeAllForUser(session.getUserId(), SessionStatus.REVOKED,
                SessionStatus.ACTIVE, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<SessionDetail> activeSessions(String sessionId) {
        UserSession current = sessions.findById(sessionId)
                .orElseThrow(() -> ApiException.unauthorized("SESSION_INVALID", "Unknown session"));
        return sessions.findByUserIdAndStatus(current.getUserId(), SessionStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(UserSession::getIssuedAt).reversed())
                .map(s -> new SessionDetail(s.getSessionId(), s.getUserId(), s.getStatus().name(),
                        s.getIssuedAt(), s.getLastActivityAt(), s.getExpiresAt(), s.getIpAddress()))
                .toList();
    }

    @Transactional
    public void revokeSession(String callerSessionId, String targetSessionId) {
        UserSession caller = sessions.findById(callerSessionId)
                .orElseThrow(() -> ApiException.unauthorized("SESSION_INVALID", "Unknown session"));
        UserSession target = sessions.findById(targetSessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "No such session"));
        if (!caller.getUserId().equals(target.getUserId())) {
            throw ApiException.forbidden("SESSION_NOT_OWNED", "A session may only be revoked by its owner");
        }
        target.setStatus(SessionStatus.REVOKED);
        target.setRevokedAt(Instant.now());
        sessions.save(target);
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
