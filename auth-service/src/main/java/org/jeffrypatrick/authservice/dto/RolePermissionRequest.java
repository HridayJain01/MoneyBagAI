package org.jeffrypatrick.authservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RolePermissionRequest(
        @NotNull
        Long roleId,

        @NotNull
        Set<Long> permissionIds
) {
}
