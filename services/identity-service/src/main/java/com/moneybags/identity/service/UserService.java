package com.moneybags.identity.service;

import com.moneybags.identity.api.ApiModels.*;
import com.moneybags.identity.entity.*;
import com.moneybags.identity.entity.SessionStatus;
import com.moneybags.identity.entity.UserStatus;
import com.moneybags.identity.repository.*;
import com.moneybags.identity.support.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final SessionRepository sessions;

    /**
     * customer-service probes this before creating a customer. It discards the body
     * and only distinguishes 200 from non-2xx, so the status code is the real contract.
     */
    @Transactional(readOnly = true)
    public UserSummary summary(Long userId) {
        User user = require(userId);
        return new UserSummary(user.getUserId(), user.getUsername(), user.getStatus().name());
    }

    @Transactional(readOnly = true)
    public UserDetail detail(Long userId) {
        return toDetail(require(userId));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDetail> search(UserStatus status, String branchCode, String search,
                                           int page, int size) {
        Page<User> result = users.search(status, blankToNull(branchCode), blankToNull(search),
                PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::toDetail).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public UserDetail create(CreateUserRequest request, String passwordHash) {
        if (users.existsByUsername(request.username())) {
            throw ApiException.conflict("USERNAME_TAKEN", "Username already exists");
        }
        if (users.existsByEmail(request.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email already exists");
        }

        Set<Role> assigned = new LinkedHashSet<>();
        if (request.roles() != null) {
            for (String roleName : request.roles()) {
                assigned.add(roles.findByRoleName(roleName)
                        .orElseThrow(() -> ApiException.invalid("UNKNOWN_ROLE", "No such role: " + roleName)));
            }
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordHash)
                .fullName(request.fullName())
                .mobile(request.mobile())
                .status(UserStatus.ACTIVE)
                .failedAttempts(0)
                .employeeId(request.employeeId())
                .branchCode(request.branchCode())
                .passwordChangedAt(Instant.now())
                .roles(assigned)
                .build();
        return toDetail(users.save(user));
    }

    /**
     * Called by branch-employee-service when an employee is created, so the gateway
     * can resolve employee and branch from the session in a single hop.
     */
    @Transactional
    public UserDetail setEmployment(Long userId, EmploymentRequest request) {
        User user = require(userId);
        user.setEmployeeId(request.employeeId());
        user.setBranchCode(request.branchCode());
        return toDetail(users.save(user));
    }

    @Transactional
    public UserDetail lock(Long userId, long minutes) {
        User user = require(userId);
        user.setStatus(UserStatus.LOCKED);
        user.setLockedUntil(Instant.now().plus(minutes, ChronoUnit.MINUTES));
        sessions.revokeAllForUser(userId, SessionStatus.REVOKED, SessionStatus.ACTIVE, Instant.now());
        return toDetail(users.save(user));
    }

    @Transactional
    public UserDetail unlock(Long userId) {
        User user = require(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setLockedUntil(null);
        user.setFailedAttempts(0);
        return toDetail(users.save(user));
    }

    @Transactional
    public UserDetail disable(Long userId) {
        User user = require(userId);
        user.setStatus(UserStatus.DISABLED);
        // Disabling without revoking sessions would leave the user working until TTL.
        sessions.revokeAllForUser(userId, SessionStatus.REVOKED, SessionStatus.ACTIVE, Instant.now());
        return toDetail(users.save(user));
    }

    @Transactional
    public UserDetail enable(Long userId) {
        User user = require(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        return toDetail(users.save(user));
    }

    @Transactional
    public UserDetail assignRole(Long userId, Long roleId) {
        User user = require(userId);
        Role role = roles.findById(roleId)
                .orElseThrow(() -> ApiException.notFound("ROLE_NOT_FOUND", "No such role"));
        user.getRoles().add(role);
        return toDetail(users.save(user));
    }

    @Transactional
    public UserDetail removeRole(Long userId, Long roleId) {
        User user = require(userId);
        user.getRoles().removeIf(role -> role.getRoleId().equals(roleId));
        return toDetail(users.save(user));
    }

    @Transactional(readOnly = true)
    public List<RoleDetail> listRoles() {
        return roles.findAll().stream()
                .map(role -> new RoleDetail(role.getRoleId(), role.getRoleName(), role.getDescription(),
                        role.getPermissions().stream().map(Permission::getPermissionCode).sorted().toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionDetail> listPermissions() {
        return permissions.findAll().stream()
                .map(p -> new PermissionDetail(p.getPermissionId(), p.getPermissionCode(),
                        p.getDescription(), p.getServiceName(), p.getAction()))
                .toList();
    }

    private User require(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "No user with id " + userId));
    }

    private UserDetail toDetail(User user) {
        return new UserDetail(
                user.getUserId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getMobile(), user.getStatus().name(), user.getEmployeeId(), user.getBranchCode(),
                user.getLastLoginAt(), user.getCreatedAt(),
                user.getRoles().stream().map(Role::getRoleName).sorted().toList(),
                user.permissionCodes().stream().sorted().toList());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
