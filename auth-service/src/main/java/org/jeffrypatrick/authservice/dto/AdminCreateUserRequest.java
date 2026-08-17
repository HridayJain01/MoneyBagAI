package org.jeffrypatrick.authservice.dto;

import jakarta.validation.constraints.*;
import org.jeffrypatrick.authservice.model.Gender;
import org.jeffrypatrick.authservice.model.RoleName;

import java.time.LocalDate;

public record AdminCreateUserRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Past LocalDate dob,
        Gender gender,
        @Size(max = 20) String mobile,
        @NotNull RoleName role
) {
}
