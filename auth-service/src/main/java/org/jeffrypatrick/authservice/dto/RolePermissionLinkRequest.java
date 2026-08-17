package org.jeffrypatrick.authservice.dto;

import jakarta.validation.constraints.NotNull;

public record RolePermissionLinkRequest(
        @NotNull
        Long roleId,

        @NotNull
        Long permissionId
) {
}
