package com.moneybags.customer.dto;

import com.moneybags.customer.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;

public final class CustomerOperations {
    private CustomerOperations() {}
    public record Update(@NotBlank String firstName, String lastName, @Past LocalDate dob, Gender gender, @NotBlank @Pattern(regexp="[6-9][0-9]{9}") String mobile, @Email String email) {}
    public record Address(@NotNull AddressType addressType, @NotBlank String line1, @NotBlank String city, @NotBlank String state, @NotBlank String pincode, @NotBlank String country, @NotNull Boolean isCurrent) {}
    public record KycSubmit(@NotBlank String docType, @NotBlank String docNumber, LocalDate expiryDate, @NotBlank String filePath) {}
    public record KycDecision(@NotNull DocumentVerifyStatus status, Long employeeId, String rejectionReason) {}
    public record BeneficiaryRequest(@NotBlank String beneficiaryName, @NotBlank String beneficiaryAccountNo, String beneficiaryBankName, @NotBlank String beneficiaryIfsc, String beneficiaryNickname, @NotBlank String beneficiaryType) {}
}
