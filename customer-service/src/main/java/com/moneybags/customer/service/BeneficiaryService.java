package com.moneybags.customer.service;

import com.moneybags.customer.dto.CustomerOperations.BeneficiaryRequest;
import com.moneybags.customer.entity.Beneficiary;

import java.util.List;
import java.util.Map;

public interface BeneficiaryService {

    Beneficiary getByIdAndCif(
            String cif,
            Long beneficiaryId
    );

    Beneficiary add(
            String cif,
            BeneficiaryRequest request
    );

    Beneficiary update(
            String cif,
            Long beneficiaryId,
            BeneficiaryRequest request
    );

    Beneficiary activate(
            String cif,
            Long beneficiaryId
    );

    Map<String, Object> eligibility(
            String cif,
            Long beneficiaryId
    );

    Beneficiary setBlocked(
            String cif,
            Long beneficiaryId,
            boolean blocked
    );

    void remove(
            String cif,
            Long beneficiaryId
    );

    List<Beneficiary> findByCustomer(
            String cif,
            String status
    );

    List<Beneficiary> findByCustomer(
            String cif,
            String status,
            String beneficiaryType
    );
}
