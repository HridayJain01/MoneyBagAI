package com.moneybags.customer.dto;

import com.moneybags.customer.enums.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CustomerRequest(
        @Positive Long userId,
        @NotBlank @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @NotNull @Past LocalDate dob,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "[6-9][0-9]{9}") String mobile,
        @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]") String panNo,
        @NotNull CustomerStatus status,
        @NotNull KycStatus kycStatus
) {}
