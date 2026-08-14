package com.moneybags.customer.service.impl;

import com.moneybags.customer.entity.KycRejectionHistory;
import com.moneybags.customer.exception.ResourceNotFoundException;
import com.moneybags.customer.repository.CustomerRepository;
import com.moneybags.customer.repository.KycRejectionHistoryRepository;
import com.moneybags.customer.service.KycDocumentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class KycDocumentHistoryServiceImpl
        implements KycDocumentHistoryService {

    private final KycRejectionHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;

    @Override
    public KycRejectionHistory recordRejection(
            String cif,
            Long documentId,
            String failureReason,
            Long rejectedByEmployeeId,
            Integer attemptNumber
    ) {
        KycRejectionHistory history =
                KycRejectionHistory.builder()
                        .cifNo(cif)
                        .docId(documentId)
                        .failureReason(failureReason)
                        .rejectedByEmpId(
                                rejectedByEmployeeId
                        )
                        .attemptNumber(attemptNumber)
                        .build();

        return historyRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycRejectionHistory> findByCustomer(
            String cif
    ) {
        if (!customerRepository.existsById(cif)) {
            throw new ResourceNotFoundException(
                    "Customer not found: " + cif
            );
        }

        return historyRepository
                .findByCifNoOrderByRejectedAtDesc(cif);
    }
}