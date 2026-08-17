package com.moneybags.customer.service.impl;

import com.moneybags.customer.dto.CustomerOperations.Address;
import com.moneybags.customer.dto.CustomerOperations.KycDecision;
import com.moneybags.customer.dto.CustomerOperations.KycSubmit;
import com.moneybags.customer.dto.CustomerOperations.Update;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.entity.CustomerAddress;
import com.moneybags.customer.entity.KycDocument;
import com.moneybags.customer.entity.KycRejectionHistory;
import com.moneybags.customer.enums.CustomerStatus;
import com.moneybags.customer.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Backward-compatible facade for callers that used the original combined
 * customer operations service. Business rules live in the focused services.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerOperationsServiceImpl implements CustomerOperationsService {
    private final CustomerService customerService;
    private final CustomerAddressService addressService;
    private final KycDocumentService kycDocumentService;
    private final KycDocumentHistoryService kycHistoryService;

    @Override public Customer update(String cif, Update request) { return customerService.update(cif, request); }
    @Override public Map<String, Object> summary(String cif) { return customerService.summary(cif); }
    @Override public Map<String, Integer> completeness(String cif) { return customerService.completeness(cif); }
    @Override public Customer setStatus(String cif, CustomerStatus status) { return customerService.setStatus(cif, status); }
    @Override public Customer assignManager(String cif, Long employeeId) { return customerService.assignManager(cif, employeeId); }
    @Override public Customer removeManager(String cif) { return customerService.removeManager(cif); }
    @Override @Transactional(readOnly = true) public List<Customer> byManager(Long employeeId) { return customerService.findByManager(employeeId); }
    @Override public CustomerAddress addAddress(String cif, Address request) { return addressService.add(cif, request); }
//    @Override public CustomerAddress updateAddress(String cif, Long addressId, Address request) { return addressService.update(cif, addressId, request); }
//    @Override public void removeAddress(String cif, Long addressId) { addressService.remove(cif, addressId); }
    @Override @Transactional(readOnly = true) public List<CustomerAddress> addresses(String cif) { return addressService.findByCustomer(cif); }
    @Override public KycDocument submitKyc(String cif, KycSubmit request) { return kycDocumentService.submit(cif, request); }
    @Override public KycDocument assignKyc(String cif, Long documentId, Long employeeId) { return kycDocumentService.assign(cif, documentId, employeeId); }
    @Override public KycDocument decideKyc(String cif, Long documentId, KycDecision request) { return kycDocumentService.decide(cif, documentId, request); }
    @Override @Transactional(readOnly = true) public List<KycDocument> pendingKyc() { return kycDocumentService.findPending(); }
    @Override @Transactional(readOnly = true) public List<KycDocument> reKycRequired() { return kycDocumentService.findReKycRequired(); }
    @Override @Transactional(readOnly = true) public List<KycRejectionHistory> kycHistory(String cif) { return kycHistoryService.findByCustomer(cif); }
}
