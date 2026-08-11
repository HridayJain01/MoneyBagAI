package com.moneybags.customer.service;

import com.moneybags.customer.dto.CustomerOperations.KycDecision;
import com.moneybags.customer.dto.CustomerOperations.KycSubmit;
import com.moneybags.customer.entity.KycDocument;

import java.util.List;
import java.time.LocalDate;

public interface KycDocumentService {

    KycDocument getByIdAndCif(
            String cif,
            Long documentId
    );

    KycDocument submit(
            String cif,
            KycSubmit request
    );

    KycDocument assign(
            String cif,
            Long documentId,
            Long employeeId
    );

    KycDocument decide(
            String cif,
            Long documentId,
            KycDecision request
    );

    List<KycDocument> findPending();

    List<KycDocument> findReKycRequired();

    List<KycDocument> findByCustomer(String cif);

    int processExpiryAlerts(LocalDate asOfDate);
}
