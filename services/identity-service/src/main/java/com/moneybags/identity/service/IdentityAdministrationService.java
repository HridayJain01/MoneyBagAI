package com.moneybags.identity.service;

import com.moneybags.identity.api.ApiModels.PermissionDetail;
import com.moneybags.identity.api.ApiModels.PermissionRequest;
import com.moneybags.identity.api.ApiModels.RoleDetail;
import com.moneybags.identity.api.ApiModels.RoleRequest;
import com.moneybags.identity.entity.Permission;
import com.moneybags.identity.entity.Role;
import com.moneybags.identity.repository.PermissionRepository;
import com.moneybags.identity.repository.RoleRepository;
import com.moneybags.identity.repository.UserRepository;
import com.moneybags.identity.support.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IdentityAdministrationService {

    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRepository users;

    @Transactional
    public RoleDetail createRole(RoleRequest request) {
        if (roles.findByRoleName(request.roleName()).isPresent()) {
            throw ApiException.conflict("ROLE_EXISTS", "Role already exists: " + request.roleName());
        }
        return toRole(roles.save(Role.builder()
                .roleName(request.roleName())
                .description(request.description())
                .build()));
    }

    @Transactional(readOnly = true)
    public RoleDetail role(Long roleId) {
        return toRole(requireRole(roleId));
    }

    @Transactional
    public RoleDetail updateRole(Long roleId, RoleRequest request) {
        Role role = requireRole(roleId);
        roles.findByRoleName(request.roleName())
                .filter(existing -> !existing.getRoleId().equals(roleId))
                .ifPresent(existing -> {
                    throw ApiException.conflict("ROLE_EXISTS", "Role already exists: " + request.roleName());
                });
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        return toRole(roles.save(role));
    }

    @Transactional
    public void deleteRole(Long roleId) {
        Role role = requireRole(roleId);
        if (users.existsByRoles_RoleId(roleId)) {
            throw ApiException.conflict("ROLE_IN_USE", "Remove this role from all users before deleting it");
        }
        role.getPermissions().clear();
        roles.save(role);
        roles.delete(role);
    }

    @Transactional
    public PermissionDetail createPermission(PermissionRequest request) {
        if (permissions.findByPermissionCode(request.permissionCode()).isPresent()) {
            throw ApiException.conflict("PERMISSION_EXISTS",
                    "Permission already exists: " + request.permissionCode());
        }
        return toPermission(permissions.save(Permission.builder()
                .permissionCode(request.permissionCode())
                .description(request.description())
                .serviceName(request.serviceName())
                .action(request.action())
                .build()));
    }

    @Transactional(readOnly = true)
    public PermissionDetail permission(Long permissionId) {
        return toPermission(requirePermission(permissionId));
    }

    @Transactional
    public PermissionDetail updatePermission(Long permissionId, PermissionRequest request) {
        Permission permission = requirePermission(permissionId);
        permissions.findByPermissionCode(request.permissionCode())
                .filter(existing -> !existing.getPermissionId().equals(permissionId))
                .ifPresent(existing -> {
                    throw ApiException.conflict("PERMISSION_EXISTS",
                            "Permission already exists: " + request.permissionCode());
                });
        permission.setPermissionCode(request.permissionCode());
        permission.setDescription(request.description());
        permission.setServiceName(request.serviceName());
        permission.setAction(request.action());
        return toPermission(permissions.save(permission));
    }

    @Transactional
    public void deletePermission(Long permissionId) {
        Permission permission = requirePermission(permissionId);
        if (roles.existsByPermissions_PermissionId(permissionId)) {
            throw ApiException.conflict("PERMISSION_IN_USE",
                    "Unlink this permission from all roles before deleting it");
        }
        permissions.delete(permission);
    }

    @Transactional
    public RoleDetail linkPermission(Long roleId, Long permissionId) {
        Role role = requireRole(roleId);
        role.getPermissions().add(requirePermission(permissionId));
        return toRole(roles.save(role));
    }

    @Transactional
    public RoleDetail replacePermissions(Long roleId, Set<Long> permissionIds) {
        Role role = requireRole(roleId);
        Set<Permission> replacement = new LinkedHashSet<>();
        permissionIds.forEach(id -> replacement.add(requirePermission(id)));
        role.setPermissions(replacement);
        return toRole(roles.save(role));
    }

    @Transactional
    public RoleDetail unlinkPermission(Long roleId, Long permissionId) {
        Role role = requireRole(roleId);
        role.getPermissions().removeIf(permission -> permission.getPermissionId().equals(permissionId));
        return toRole(roles.save(role));
    }

    private Role requireRole(Long roleId) {
        return roles.findById(roleId)
                .orElseThrow(() -> ApiException.notFound("ROLE_NOT_FOUND", "No role with id " + roleId));
    }

    private Permission requirePermission(Long permissionId) {
        return permissions.findById(permissionId)
                .orElseThrow(() -> ApiException.notFound(
                        "PERMISSION_NOT_FOUND", "No permission with id " + permissionId));
    }

    private RoleDetail toRole(Role role) {
        return new RoleDetail(role.getRoleId(), role.getRoleName(), role.getDescription(),
                role.getPermissions().stream().map(Permission::getPermissionCode).sorted().toList());
    }

    private PermissionDetail toPermission(Permission permission) {
        return new PermissionDetail(permission.getPermissionId(), permission.getPermissionCode(),
                permission.getDescription(), permission.getServiceName(), permission.getAction());
    }
}
