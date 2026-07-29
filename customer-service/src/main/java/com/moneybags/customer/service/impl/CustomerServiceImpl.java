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

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final SecurityClient securityClient;

    @Override @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (repository.existsByPanNo(request.panNo())) throw new ConflictException("PAN already exists");
        securityClient.findUser(request.userId());
        // TODO enrich Feign error decoding so an unknown user becomes a local 404 response.
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }
    @Override
    public CustomerResponse findByCif(Long cifNo) {
        return mapper.toResponse(repository.findById(cifNo)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + cifNo)));
    }
    @Override
    public List<CustomerResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }
}
