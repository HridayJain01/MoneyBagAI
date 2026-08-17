package org.jeffrypatrick.authservice.controller;

import jakarta.validation.Valid;
import org.jeffrypatrick.authservice.dto.RolePermissionLinkRequest;
import org.jeffrypatrick.authservice.dto.RolePermissionRequest;
import org.jeffrypatrick.authservice.dto.RoleResponse;
import org.jeffrypatrick.authservice.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/role-permissions")
public class RolePermissionController {

    private final RoleService roleService;

    public RolePermissionController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> link(@Valid @RequestBody RolePermissionLinkRequest request) {
        return ResponseEntity.ok(roleService.linkPermission(request.roleId(), request.permissionId()));
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponse> getRolePermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getById(roleId));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponse> replaceAll(
            @PathVariable Long roleId,
            @Valid @RequestBody RolePermissionRequest request
    ) {
        return ResponseEntity.ok(roleService.replacePermissions(roleId, request.permissionIds()));
    }

    @DeleteMapping("/{roleId}/{permissionId}")
    public ResponseEntity<RoleResponse> unlink(
            @PathVariable Long roleId,
            @PathVariable Long permissionId
    ) {
        return ResponseEntity.ok(roleService.unlinkPermission(roleId, permissionId));
    }
}