package org.jeffrypatrick.authservice.service;

import org.jeffrypatrick.authservice.dto.PermissionRequest;
import org.jeffrypatrick.authservice.dto.PermissionResponse;
import org.jeffrypatrick.authservice.model.Permission;
import org.jeffrypatrick.authservice.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    //    Constructor Injection
    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public PermissionResponse create(PermissionRequest permissionRequest) {
        permissionRepository.findByName(permissionRequest.name()).ifPresent(p -> {
            throw new IllegalArgumentException(String.format("Permission with name %s already exists", permissionRequest.name()));
        });

        Permission permission = new Permission();
        permission.setName(permissionRequest.name());
        permission.setDescription(permissionRequest.description());

        Permission saved = permissionRepository.save(permission);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAll() {
        return permissionRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PermissionResponse getById(Long id) {
        Permission permission = permissionRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Permission with id %s does not exist", id)));
        return toResponse(permission);
    }

    @Transactional
    public PermissionResponse update(Long id, PermissionRequest permissionRequest) {
        Permission permission = permissionRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Permission with id %s does not exist", id)));

        permission.setName(permissionRequest.name());
        permission.setDescription(permissionRequest.description());

        return toResponse(permissionRepository.save(permission));
    }

    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Permission with id %s does not exist", id)));
        permissionRepository.delete(permission);
    }

    private PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getName(), permission.getDescription());
    }
}
