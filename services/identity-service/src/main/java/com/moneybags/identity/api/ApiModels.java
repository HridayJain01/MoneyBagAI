package com.moneybags.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Wire contracts.
 *
 * <p>Two of these shapes are frozen by existing consumers and must not drift:
 * <ul>
 *   <li>{@link UserSummary} -- customer-service's {@code SecurityClient.UserSummary}
 *       record binds to it field-for-field.</li>
 *   <li>{@link SessionPrincipal} -- the api-gateway session filter binds to it.</li>
 * </ul>
 */
public final class ApiModels {
    private ApiModels() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record LoginResponse(
            String sessionId,
            Instant expiresAt,
            Long userId,
            String username,
            String fullName,
            String employeeId,
            String branchCode,
            List<String> roles,
            List<String> permissions) {
    }

    /** Gateway -> identity. POST, not GET, so the session id never lands in an access log. */
    public record ResolveRequest(@NotBlank String sessionId) {
    }

    /**
     * What the gateway turns into downstream actor headers. {@code branchCode} is
     * emitted as BOTH X-Branch-Code and X-Branch-Id -- transaction-service reads the
     * former, statement-reporting-service the latter, and they must agree.
     */
    public record SessionPrincipal(
            Long userId,
            String username,
            String employeeId,
            String branchCode,
            List<String> permissions,
            String status,
            Instant expiresAt) {
    }

    /**
     * Frozen by customer-service's SecurityClient. {@code userId} is a Long, and the
     * call is used purely as an existence probe -- the body is discarded, only the
     * status code matters. Keep the fields anyway; a 404 is the contract that counts.
     */
    public record UserSummary(
            Long userId,
            String username,
            String status) {
    }

    public record UserDetail(
            Long userId,
            String username,
            String email,
            String fullName,
            String mobile,
            String status,
            String employeeId,
            String branchCode,
            Instant lastLoginAt,
            Instant createdAt,
            List<String> roles,
            List<String> permissions) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 20) String mobile,
            @Size(max = 64) String employeeId,
            @Size(max = 20) String branchCode,
            List<String> roles) {
    }

    /** branch-employee-service calls this when it creates an employee. */
    public record EmploymentRequest(
            @Size(max = 64) String employeeId,
            @Size(max = 20) String branchCode) {
    }

    public record RoleDetail(Long roleId, String roleName, String description, List<String> permissions) {
    }

    public record PermissionDetail(Long permissionId, String permissionCode, String description,
                                   String serviceName, String action) {
    }

    public record SessionDetail(String sessionId, Long userId, String status,
                                Instant issuedAt, Instant lastActivityAt, Instant expiresAt,
                                String ipAddress) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    public record ErrorResponse(Instant timestamp, int status, String code, String message, String path) {
    }
}
