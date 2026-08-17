package org.jeffrypatrick.authservice.dto;

public record AuthResponse(
        String message,
        UserInfoResponse user
) {
}
