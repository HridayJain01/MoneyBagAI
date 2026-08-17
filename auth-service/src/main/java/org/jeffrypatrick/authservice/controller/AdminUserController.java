package org.jeffrypatrick.authservice.controller;

import org.jeffrypatrick.authservice.dto.AdminCreateUserRequest;
import org.jeffrypatrick.authservice.dto.UpdateUserRoleRequest;
import org.jeffrypatrick.authservice.dto.UserInfoResponse;
import org.jeffrypatrick.authservice.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping
    public ResponseEntity<UserInfoResponse> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.ok(adminUserService.createUser(request));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserInfoResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return ResponseEntity.ok(adminUserService.changeRole(id, request));
    }
}