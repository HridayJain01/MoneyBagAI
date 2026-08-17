package org.jeffrypatrick.authservice.dto;

import org.jeffrypatrick.authservice.model.RoleName;

import java.util.Set;

public record RoleResponse(
        Long id,
        RoleName name,
        String description,
        Set<PermissionResponse> permissions
) {
}
