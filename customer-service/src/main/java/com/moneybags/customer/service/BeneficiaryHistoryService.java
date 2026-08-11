package com.moneybags.customer.service;

import com.moneybags.customer.entity.Beneficiary;
import com.moneybags.customer.entity.BeneficiaryChangeHistory;

import java.util.List;

public interface BeneficiaryHistoryService {

    BeneficiaryChangeHistory record(
            Beneficiary beneficiary,
            String changeType
    );

    List<BeneficiaryChangeHistory> findByBeneficiaryId(
            Long beneficiaryId
    );
}