package com.moneybags.customer.service;

import com.moneybags.customer.entity.KycRejectionHistory;

import java.util.List;

public interface KycDocumentHistoryService {

    KycRejectionHistory recordRejection(
            String cif,
            Long documentId,
            String failureReason,
            Long rejectedByEmployeeId,
            Integer attemptNumber
    );

    List<KycRejectionHistory> findByCustomer(String cif);
}