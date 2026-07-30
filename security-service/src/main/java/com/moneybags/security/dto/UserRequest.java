package com.moneybags.security.dto;

import com.moneybags.security.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 12, max = 255) String passwordHash,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 20) String mobile,
        @NotNull UserStatus status
) {
}
