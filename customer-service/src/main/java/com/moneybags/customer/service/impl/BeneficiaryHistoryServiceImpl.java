package com.moneybags.customer.service.impl;

import com.moneybags.customer.entity.Beneficiary;
import com.moneybags.customer.entity.BeneficiaryChangeHistory;
import com.moneybags.customer.repository.BeneficiaryChangeHistoryRepository;
import com.moneybags.customer.service.BeneficiaryHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BeneficiaryHistoryServiceImpl
        implements BeneficiaryHistoryService {

    private final BeneficiaryChangeHistoryRepository
            historyRepository;

    @Override
    public BeneficiaryChangeHistory record(
            Beneficiary beneficiary,
            String changeType
    ) {
        BeneficiaryChangeHistory history =
                BeneficiaryChangeHistory.builder()
                        .beneficiaryId(
                                beneficiary.getBeneficiaryId()
                        )
                        .cifNo(
                                beneficiary
                                        .getCustomer()
                                        .getCifNo()
                        )
                        .changeType(changeType)
                        .build();

        return historyRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryChangeHistory> findByBeneficiaryId(
            Long beneficiaryId
    ) {
        return historyRepository
                .findByBeneficiaryIdOrderByChangedAtDesc(
                        beneficiaryId
                );
    }
}