package org.jeffrypatrick.authservice.dto;

import jakarta.validation.constraints.NotNull;
import org.jeffrypatrick.authservice.model.RoleName;

public record UpdateUserRoleRequest(
        @NotNull RoleName role
) {
}
