package org.jeffrypatrick.authservice.service;

import org.jeffrypatrick.authservice.dto.PermissionResponse;
import org.jeffrypatrick.authservice.dto.RoleRequest;
import org.jeffrypatrick.authservice.dto.RoleResponse;
import org.jeffrypatrick.authservice.model.Permission;
import org.jeffrypatrick.authservice.model.Role;
import org.jeffrypatrick.authservice.repository.PermissionRepository;
import org.jeffrypatrick.authservice.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    //    Constructor Injection
    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        roleRepository.findByName(request.name()).ifPresent(role -> {
            throw new IllegalArgumentException("Role already exists");
        });

        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAll() {
        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        return toResponse(role);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        role.setName(request.name());
        role.setDescription(request.description());

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        roleRepository.delete(role);
    }

    @Transactional
    public RoleResponse replacePermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Set<Permission> permissions = permissionIds.stream()
                .map(permissionId -> permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionId)))
                .collect(java.util.stream.Collectors.toSet());

        role.setPermissions(permissions);

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse linkPermission(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

        role.getPermissions().add(permission);

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse unlinkPermission(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

        role.getPermissions().remove(permission);

        return toResponse(roleRepository.save(role));
    }

    private RoleResponse toResponse(Role role) {
        Set<PermissionResponse> permissionResponses = role.getPermissions().stream()
                .map(permission -> new PermissionResponse(
                        permission.getId(),
                        permission.getName(),
                        permission.getDescription()
                ))
                .collect(java.util.stream.Collectors.toSet());

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                permissionResponses
        );
    }
}
