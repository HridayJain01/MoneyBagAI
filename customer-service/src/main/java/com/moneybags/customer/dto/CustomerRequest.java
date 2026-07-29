package com.moneybags.customer.dto;

import com.moneybags.customer.enums.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CustomerRequest(
        @NotNull @Positive Long userId,
        @NotNull @Past LocalDate dob,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]") String panNo,
        @NotNull CustomerStatus status,
        @NotNull KycStatus kycStatus
) {}
