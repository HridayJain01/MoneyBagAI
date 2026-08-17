package com.moneybags.identity.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.moneybags.identity.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Wire contracts.
 *
 * <p>Two of these shapes are frozen by existing consumers and must not drift:
 * <ul>
 *   <li>{@link UserSummary} -- customer-service's {@code SecurityClient.UserSummary}
 *       record binds to it field-for-field.</li>
 * </ul>
 */
public final class ApiModels {
    private ApiModels() {
    }

    public record LoginRequest(
            @NotBlank @JsonAlias("email") String username,
            @NotBlank String password) {
    }

    /** Public self-registration contract retained from the standalone auth-service. */
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Past LocalDate dob,
            Gender gender,
            @Size(max = 20) String mobile) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            Long userId,
            String username,
            String fullName,
            String employeeId,
            String branchCode,
            List<String> roles,
            List<String> permissions) {
    }

    public record RegistrationResponse(String message, UserProfile user) {
    }

    public record UserProfile(
            Long userId,
            String username,
            String firstName,
            String lastName,
            String fullName,
            String email,
            LocalDate dob,
            Gender gender,
            String mobile,
            String status,
            List<String> roles,
            List<String> permissions,
            Instant createdAt) {
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

    /** Compatibility contract for the standalone auth-service admin endpoint. */
    public record AdminCreateUserRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Past LocalDate dob,
            Gender gender,
            @Size(max = 20) String mobile,
            @NotBlank String role) {
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

    public record RoleRequest(
            @NotBlank @JsonAlias("name") @Size(max = 50) String roleName,
            @Size(max = 255) String description) {
    }

    public record PermissionRequest(
            @NotBlank @JsonAlias("name") @Size(max = 60) String permissionCode,
            @Size(max = 255) String description,
            @Size(max = 60) String serviceName,
            @Size(max = 20) String action) {
    }

    public record RolePermissionLinkRequest(@NotNull Long roleId, @NotNull Long permissionId) {
    }

    public record RolePermissionRequest(@NotNull Set<Long> permissionIds) {
    }

    public record UpdateUserRoleRequest(@NotBlank String role) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    public record ErrorResponse(Instant timestamp, int status, String code, String message, String path) {
    }
}
