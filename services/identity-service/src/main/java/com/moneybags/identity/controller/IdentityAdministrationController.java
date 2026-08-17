package com.moneybags.identity.controller;

import com.moneybags.identity.api.ApiModels.PermissionDetail;
import com.moneybags.identity.api.ApiModels.PermissionRequest;
import com.moneybags.identity.api.ApiModels.RoleDetail;
import com.moneybags.identity.api.ApiModels.RolePermissionLinkRequest;
import com.moneybags.identity.api.ApiModels.RolePermissionRequest;
import com.moneybags.identity.api.ApiModels.RoleRequest;
import com.moneybags.identity.security.TrustedHeaderAuthorization;
import com.moneybags.identity.service.IdentityAdministrationService;
import com.moneybags.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IdentityAdministrationController {

    private static final String MANAGE = "ROLE_PERMISSION_MANAGE";

    private final IdentityAdministrationService administration;
    private final UserService users;
    private final TrustedHeaderAuthorization authorization;

    @GetMapping("/api/v1/admin/roles")
    public List<RoleDetail> roles(@RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return users.listRoles();
    }

    @GetMapping({"/api/v1/roles/{roleId}", "/api/v1/admin/roles/{roleId}"})
    public RoleDetail role(@PathVariable Long roleId,
                           @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.role(roleId);
    }

    @PostMapping({"/api/v1/roles", "/api/v1/admin/roles"})
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDetail createRole(@Valid @RequestBody RoleRequest request,
                                 @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.createRole(request);
    }

    @PutMapping({"/api/v1/roles/{roleId}", "/api/v1/admin/roles/{roleId}"})
    public RoleDetail updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleRequest request,
                                 @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.updateRole(roleId, request);
    }

    @DeleteMapping({"/api/v1/roles/{roleId}", "/api/v1/admin/roles/{roleId}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable Long roleId,
                           @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        administration.deleteRole(roleId);
    }

    @GetMapping("/api/v1/admin/permissions")
    public List<PermissionDetail> permissions(
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return users.listPermissions();
    }

    @GetMapping({"/api/v1/permissions/{permissionId}", "/api/v1/admin/permissions/{permissionId}"})
    public PermissionDetail permission(@PathVariable Long permissionId,
                                       @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.permission(permissionId);
    }

    @PostMapping({"/api/v1/permissions", "/api/v1/admin/permissions"})
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionDetail createPermission(@Valid @RequestBody PermissionRequest request,
                                             @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.createPermission(request);
    }

    @PutMapping({"/api/v1/permissions/{permissionId}", "/api/v1/admin/permissions/{permissionId}"})
    public PermissionDetail updatePermission(
            @PathVariable Long permissionId, @Valid @RequestBody PermissionRequest request,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.updatePermission(permissionId, request);
    }

    @DeleteMapping({"/api/v1/permissions/{permissionId}", "/api/v1/admin/permissions/{permissionId}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermission(@PathVariable Long permissionId,
                                 @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        administration.deletePermission(permissionId);
    }

    @PostMapping("/api/v1/admin/role-permissions")
    public RoleDetail linkPermission(
            @Valid @RequestBody RolePermissionLinkRequest request,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.linkPermission(request.roleId(), request.permissionId());
    }

    @GetMapping("/api/v1/admin/role-permissions/{roleId}")
    public RoleDetail rolePermissions(
            @PathVariable Long roleId,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.role(roleId);
    }

    @PutMapping("/api/v1/admin/role-permissions/{roleId}")
    public RoleDetail replacePermissions(
            @PathVariable Long roleId, @Valid @RequestBody RolePermissionRequest request,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.replacePermissions(roleId, request.permissionIds());
    }

    @DeleteMapping("/api/v1/admin/role-permissions/{roleId}/{permissionId}")
    public RoleDetail unlinkPermission(
            @PathVariable Long roleId, @PathVariable Long permissionId,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        requireManage(permissions);
        return administration.unlinkPermission(roleId, permissionId);
    }

    private void requireManage(String permissions) {
        authorization.require(permissions, MANAGE);
    }
}
