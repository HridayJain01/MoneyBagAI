package com.moneybags.identity.controller;

import com.moneybags.identity.api.ApiModels.*;
import com.moneybags.identity.entity.UserStatus;
import com.moneybags.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Frozen shape: customer-service's SecurityClient binds
     * {@code UserSummary(Long userId, String username, String status)} and treats a
     * non-2xx as "user does not exist".
     */
    @Operation(summary = "User existence probe used by customer-service")
    @GetMapping("/api/v1/users/{userId}")
    public UserSummary summary(@PathVariable Long userId) {
        return userService.summary(userId);
    }

    @Operation(summary = "Full user profile")
    @GetMapping("/api/v1/users/{userId}/detail")
    public UserDetail detail(@PathVariable Long userId) {
        return userService.detail(userId);
    }

    @Operation(summary = "Search users")
    @GetMapping("/api/v1/users")
    public PageResponse<UserDetail> search(@RequestParam(required = false) UserStatus status,
                                           @RequestParam(required = false) String branchCode,
                                           @RequestParam(required = false) String search,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "25") int size) {
        return userService.search(status, branchCode, search, page, size);
    }

    @Operation(summary = "Create a staff user")
    @PostMapping("/api/v1/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDetail create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request, passwordEncoder.encode(request.password()));
    }

    @PostMapping("/api/v1/users/{userId}/lock")
    public UserDetail lock(@PathVariable Long userId,
                           @RequestParam(defaultValue = "60") long minutes) {
        return userService.lock(userId, minutes);
    }

    @PostMapping("/api/v1/users/{userId}/unlock")
    public UserDetail unlock(@PathVariable Long userId) {
        return userService.unlock(userId);
    }

    @PostMapping("/api/v1/users/{userId}/disable")
    public UserDetail disable(@PathVariable Long userId) {
        return userService.disable(userId);
    }

    @PostMapping("/api/v1/users/{userId}/enable")
    public UserDetail enable(@PathVariable Long userId) {
        return userService.enable(userId);
    }

    @PostMapping("/api/v1/users/{userId}/roles/{roleId}")
    public UserDetail assignRole(@PathVariable Long userId, @PathVariable Long roleId) {
        return userService.assignRole(userId, roleId);
    }

    @DeleteMapping("/api/v1/users/{userId}/roles/{roleId}")
    public UserDetail removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        return userService.removeRole(userId, roleId);
    }

    /** branch-employee-service pushes employment here so the gateway can resolve it in one hop. */
    @Operation(summary = "Attach employee and branch to a user")
    @PutMapping("/internal/v1/users/{userId}/employment")
    public UserDetail setEmployment(@PathVariable Long userId,
                                    @Valid @RequestBody EmploymentRequest request) {
        return userService.setEmployment(userId, request);
    }

    @GetMapping("/api/v1/roles")
    public List<RoleDetail> roles() {
        return userService.listRoles();
    }

    @GetMapping("/api/v1/permissions")
    public List<PermissionDetail> permissions() {
        return userService.listPermissions();
    }
}
