package com.moneybags.customer.dto;
import com.moneybags.customer.enums.*;
import java.time.LocalDate;
public record CustomerResponse(Long cifNo, Long userId, LocalDate dob, Gender gender, String panNo,
                               CustomerStatus status, KycStatus kycStatus) {}
