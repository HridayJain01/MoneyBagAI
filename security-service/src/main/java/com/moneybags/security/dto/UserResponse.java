package com.moneybags.security.dto;

import com.moneybags.security.enums.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String username,
        String email,
        String fullName,
        String mobile,
        UserStatus status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
