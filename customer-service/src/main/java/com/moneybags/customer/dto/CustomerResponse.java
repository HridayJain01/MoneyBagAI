package com.moneybags.customer.dto;
import com.moneybags.customer.enums.*;
import java.time.LocalDate;
public record CustomerResponse(String cifNo, Long userId, Long relationshipManagerEmpId, String firstName, String lastName,
                               LocalDate dob, Gender gender, String mobile, String email, String panNo,
                               CustomerStatus status, KycStatus kycStatus, Integer kycFailureCount) {}
