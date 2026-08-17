package com.moneybags.customer.service;

import com.moneybags.customer.dto.CustomerOperations.Address;
import com.moneybags.customer.dto.CustomerOperations.KycDecision;
import com.moneybags.customer.dto.CustomerOperations.KycSubmit;
import com.moneybags.customer.dto.CustomerOperations.Update;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.entity.CustomerAddress;
import com.moneybags.customer.entity.KycDocument;
import com.moneybags.customer.entity.KycRejectionHistory;
import com.moneybags.customer.enums.CustomerStatus;

import java.util.List;
import java.util.Map;

public interface CustomerOperationsService {

    Customer update(String cif, Update request);

    Map<String, Object> summary(String cif);

    Map<String, Integer> completeness(String cif);

    Customer setStatus(String cif, CustomerStatus status);

    Customer assignManager(String cif, Long employeeId);

    Customer removeManager(String cif);

    List<Customer> byManager(Long employeeId);

    CustomerAddress addAddress(String cif, Address request);

//    CustomerAddress updateAddress(
//            String cif,
//            Long addressId,
//            Address request
//    );

//    void removeAddress(String cif, Long addressId);

    List<CustomerAddress> addresses(String cif);

    KycDocument submitKyc(String cif, KycSubmit request);

    KycDocument assignKyc(
            String cif,
            Long documentId,
            Long employeeId
    );

    KycDocument decideKyc(
            String cif,
            Long documentId,
            KycDecision request
    );

    List<KycDocument> pendingKyc();

    List<KycDocument> reKycRequired();

    List<KycRejectionHistory> kycHistory(String cif);

}
