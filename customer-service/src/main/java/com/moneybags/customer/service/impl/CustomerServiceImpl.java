package com.moneybags.customer.service.impl;

import com.moneybags.customer.client.SecurityClient;
import com.moneybags.customer.dto.*;
import com.moneybags.customer.exception.*;
import com.moneybags.customer.mapper.CustomerMapper;
import com.moneybags.customer.repository.CustomerRepository;
import com.moneybags.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final SecurityClient securityClient;

    @Override @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (repository.existsByPanNo(request.panNo())) throw new ConflictException("PAN already exists");
        if (request.userId() != null) {
            securityClient.findUser(request.userId());
            if (repository.existsByUserId(request.userId())) throw new ConflictException("User already has a customer profile");
        }
        // TODO enrich Feign error decoding so an unknown user becomes a local 404 response.
        var customer = mapper.toEntity(request);
        customer.setCifNo(nextCif());
        return mapper.toResponse(repository.save(customer));
    }
    @Override
    public CustomerResponse findByCif(String cifNo) {
        return mapper.toResponse(repository.findById(cifNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + cifNo)));
    }
    @Override
    public List<CustomerResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }
    private String nextCif() { return "CIF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(); }
}
