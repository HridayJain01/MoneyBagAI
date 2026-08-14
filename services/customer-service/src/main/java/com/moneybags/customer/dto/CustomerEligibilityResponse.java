package com.moneybags.customer.dto;

import com.moneybags.customer.enums.CustomerStatus;
import com.moneybags.customer.enums.KycStatus;
import com.moneybags.customer.enums.RiskClassification;

import java.util.List;

public record CustomerEligibilityResponse(
        String cifNo,
        boolean eligible,
        CustomerStatus customerStatus,
        KycStatus kycStatus,
        RiskClassification riskClassification,
        boolean adult,
        boolean residentAddressAvailable,
        List<String> reasons
) {
}
