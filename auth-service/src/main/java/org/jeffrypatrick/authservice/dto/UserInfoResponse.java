package org.jeffrypatrick.authservice.dto;

import org.jeffrypatrick.authservice.model.Gender;
import org.jeffrypatrick.authservice.model.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record UserInfoResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        LocalDate dob,
        Gender gender,
        String mobile,
        String role,
        Set<String> permissions,
        Status status,
        LocalDateTime createdAt
) {
}
