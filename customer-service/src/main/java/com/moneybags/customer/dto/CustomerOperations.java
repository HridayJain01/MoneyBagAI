package com.moneybags.customer.dto;

import com.moneybags.customer.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;

public final class CustomerOperations {
    private CustomerOperations() {}
    public record Update(@Size(max = 80) String firstName, @Size(max = 80) String lastName, @Past LocalDate dob, Gender gender, @Pattern(regexp="[6-9][0-9]{9}") String mobile, @Email @Size(max = 150) String email) {}
    public record Address(@NotNull AddressType addressType, @NotBlank String line1, @NotBlank String city, @NotBlank String state, @NotBlank @Pattern(regexp = "[1-9][0-9]{5}") String pincode, @NotBlank String country, @NotNull Boolean isCurrent) {}
    public record KycSubmit(@NotBlank String docType, @NotBlank String docNumber, LocalDate expiryDate, @NotBlank String filePath) {}
    public record KycDecision(@NotNull DocumentVerifyStatus status, Long employeeId, String rejectionReason) {}
    public record BeneficiaryRequest(@NotBlank String beneficiaryName, @NotBlank String beneficiaryAccountNo, String beneficiaryBankName, @NotBlank @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}") String beneficiaryIfsc, String beneficiaryNickname, @NotBlank @Pattern(regexp = "BANK_ACCOUNT|NOMINEE") String beneficiaryType) {}
    public record CommunicationPreferences(@NotNull CommunicationChannel preferredChannel,
                                           @NotNull Boolean emailEnabled,
                                           @NotNull Boolean smsEnabled,
                                           @NotNull Boolean pushEnabled) {}
    public record RiskUpdate(@NotNull RiskClassification riskClassification) {}
}
