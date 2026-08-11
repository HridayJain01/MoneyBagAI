package com.moneybags.customer.service.impl;

import com.moneybags.customer.client.SecurityClient;
import com.moneybags.customer.dto.CustomerEligibilityResponse;
import com.moneybags.customer.dto.CustomerOperations.CommunicationPreferences;
import com.moneybags.customer.dto.CustomerOperations.RiskUpdate;
import com.moneybags.customer.dto.CustomerOperations.Update;
import com.moneybags.customer.dto.CustomerRequest;
import com.moneybags.customer.dto.CustomerResponse;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.enums.CustomerStatus;
import com.moneybags.customer.enums.KycStatus;
import com.moneybags.customer.exception.ConflictException;
import com.moneybags.customer.exception.ResourceNotFoundException;
import com.moneybags.customer.mapper.CustomerMapper;
import com.moneybags.customer.repository.CustomerAddressRepository;
import com.moneybags.customer.repository.CustomerRepository;
import com.moneybags.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final int PROFILE_TOTAL_FIELDS = 7;

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerMapper customerMapper;
    private final SecurityClient securityClient;

    @Override
    public CustomerResponse create(CustomerRequest request) {
        System.out.println("hello");
        String pan = request.panNo().toUpperCase(Locale.ROOT);
        if (customerRepository.existsByPanNo(pan)) {
            throw new ConflictException("PAN already exists");
        }
        if (request.userId() != null) {
            securityClient.findUser(request.userId());
            if (customerRepository.existsByUserId(request.userId())) {
                throw new ConflictException("User already has a customer profile");
            }
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setCifNo(nextCif());
        customer.setPanNo(pan);
        customer.setKycStatus(KycStatus.PENDING);
        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        return customerRepository.search(query.trim()).stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getByCif(String cif) {
        return customerRepository.findById(cif)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + cif));
    }

    @Override
    public Customer update(String cif, Update request) {
        Customer customer = getByCif(cif);
        if (request.firstName() != null) customer.setFirstName(request.firstName());
        if (request.lastName() != null) customer.setLastName(request.lastName());
        if (request.dob() != null) customer.setDob(request.dob());
        if (request.gender() != null) customer.setGender(request.gender());
        if (request.mobile() != null) customer.setMobile(request.mobile());
        if (request.email() != null) customer.setEmail(request.email());
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> summary(String cif) {
        Customer customer = getByCif(cif);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("cifNo", customer.getCifNo());
        response.put("status", customer.getStatus());
        response.put("kycStatus", customer.getKycStatus());
        response.put("riskClassification", customer.getRiskClassification());
        response.put("profileCompleteness", completeness(cif));
        response.put("accountEligibility", eligibility(cif));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> completeness(String cif) {
        Customer customer = getByCif(cif);
        int completedFields = 0;
        if (customer.getFirstName() != null && !customer.getFirstName().isBlank()) completedFields++;
        if (customer.getDob() != null) completedFields++;
        if (customer.getGender() != null) completedFields++;
        if (customer.getMobile() != null && !customer.getMobile().isBlank()) completedFields++;
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) completedFields++;
        if (customer.getPanNo() != null && !customer.getPanNo().isBlank()) completedFields++;
        if (addressRepository.existsByCustomerCifNo(cif)) completedFields++;

        return Map.of(
                "completedFields", completedFields,
                "totalFields", PROFILE_TOTAL_FIELDS,
                "percentage", completedFields * 100 / PROFILE_TOTAL_FIELDS
        );
    }

    @Override
    public Customer setStatus(String cif, CustomerStatus status) {
        Customer customer = getByCif(cif);
        customer.setStatus(status);
        return customerRepository.save(customer);
    }

    @Override
    public Customer assignManager(String cif, Long employeeId) {
        Customer customer = getByCif(cif);
        customer.setRelationshipManagerEmpId(employeeId);
        return customerRepository.save(customer);
    }

    @Override
    public Customer removeManager(String cif) {
        Customer customer = getByCif(cif);
        customer.setRelationshipManagerEmpId(null);
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> findByManager(Long employeeId) {
        return customerRepository.findByRelationshipManagerEmpId(employeeId);
    }

    @Override
    public Customer updateCommunicationPreferences(String cif, CommunicationPreferences request) {
        Customer customer = getByCif(cif);
        customer.setPreferredCommunicationChannel(request.preferredChannel());
        customer.setEmailNotificationsEnabled(request.emailEnabled());
        customer.setSmsNotificationsEnabled(request.smsEnabled());
        customer.setPushNotificationsEnabled(request.pushEnabled());
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> communicationPreferences(String cif) {
        Customer customer = getByCif(cif);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("preferredChannel", customer.getPreferredCommunicationChannel());
        response.put("emailEnabled", customer.getEmailNotificationsEnabled());
        response.put("smsEnabled", customer.getSmsNotificationsEnabled());
        response.put("pushEnabled", customer.getPushNotificationsEnabled());
        return response;
    }

    @Override
    public Customer classifyRisk(String cif, RiskUpdate request) {
        Customer customer = getByCif(cif);
        customer.setRiskClassification(request.riskClassification());
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerEligibilityResponse eligibility(String cif) {
        Customer customer = getByCif(cif);
        boolean adult = customer.getDob() != null
                && Period.between(customer.getDob(), LocalDate.now()).getYears() >= 18;
        boolean residentAddressAvailable = addressRepository
                .existsByCustomerCifNoAndCountryIgnoreCase(cif, "India");

        List<String> reasons = new ArrayList<>();
        if (customer.getStatus() != CustomerStatus.ACTIVE) reasons.add("CUSTOMER_NOT_ACTIVE");
        if (customer.getKycStatus() != KycStatus.VERIFIED) reasons.add("KYC_NOT_VERIFIED");
        if (!adult) reasons.add("CUSTOMER_IS_MINOR");
        if (!residentAddressAvailable) reasons.add("INDIAN_RESIDENT_ADDRESS_REQUIRED");

        return new CustomerEligibilityResponse(
                customer.getCifNo(),
                reasons.isEmpty(),
                customer.getStatus(),
                customer.getKycStatus(),
                customer.getRiskClassification(),
                adult,
                residentAddressAvailable,
                List.copyOf(reasons)
        );
    }

    private String nextCif() {
        String cif;
        do {
            cif = "CIF" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase(Locale.ROOT);
        } while (customerRepository.existsById(cif));
        return cif;
    }
}
