package com.moneybags.customer.service;

import com.moneybags.customer.dto.CustomerRequest;
import com.moneybags.customer.dto.CustomerResponse;
import com.moneybags.customer.dto.CustomerEligibilityResponse;
import com.moneybags.customer.dto.CustomerOperations.CommunicationPreferences;
import com.moneybags.customer.dto.CustomerOperations.RiskUpdate;
import com.moneybags.customer.dto.CustomerOperations.Update;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.enums.CustomerStatus;

import java.util.List;
import java.util.Map;

public interface CustomerService {

    CustomerResponse create(CustomerRequest request);

    List<CustomerResponse> findAll();

    List<CustomerResponse> search(String query);

    Customer getByCif(String cif);

    Customer update(String cif, Update request);

    Map<String, Object> summary(String cif);

    Map<String, Integer> completeness(String cif);

    Customer setStatus(String cif, CustomerStatus status);

    Customer assignManager(String cif, Long employeeId);

    Customer removeManager(String cif);

    List<Customer> findByManager(Long employeeId);

    Customer updateCommunicationPreferences(String cif, CommunicationPreferences request);

    Map<String, Object> communicationPreferences(String cif);

    Customer classifyRisk(String cif, RiskUpdate request);

    CustomerEligibilityResponse eligibility(String cif);
}
