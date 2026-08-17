package org.jeffrypatrick.authservice.dto;

public record PermissionResponse(
        Long id,
        String name,
        String description
) {
}
