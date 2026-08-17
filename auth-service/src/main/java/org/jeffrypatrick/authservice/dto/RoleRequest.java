package org.jeffrypatrick.authservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jeffrypatrick.authservice.model.RoleName;

public record RoleRequest(
        @NotNull
        RoleName name,

        @Size(max = 500)
        String description
) {
}
