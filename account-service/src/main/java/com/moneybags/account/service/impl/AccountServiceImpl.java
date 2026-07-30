package com.moneybags.account.service.impl;

import com.moneybags.account.client.*;
import com.moneybags.account.dto.*;
import com.moneybags.account.entity.Account;
import com.moneybags.account.enums.AccountStatus;
import com.moneybags.account.exception.*;
import com.moneybags.account.mapper.AccountMapper;
import com.moneybags.account.repository.AccountRepository;
import com.moneybags.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {
    private final AccountRepository repository;
    private final AccountMapper mapper;
    private final CustomerClient customerClient;
    private final ProductClient productClient;
    private final SecurityClient securityClient;

    @Override @Transactional
    public AccountResponse create(AccountRequest request) {
        if (repository.existsById(request.accountNo())) throw new ConflictException("Account already exists");
        customerClient.findCustomer(Long.valueOf(request.cifNo()));
        ProductClient.ProductSummary product = productClient.findProduct(request.productCode());
        securityClient.findBranch(request.branchCode());
        Account account = mapper.toEntity(request);
        account.setMinBalance(product.minBalance());
        account.setStatus(AccountStatus.PENDING_APPROVAL);
        account.setOpenedOn(LocalDate.now());
        // TODO enforce KYC/product/branch ACTIVE states and create the maker-checker approval workflow.
        return mapper.toResponse(repository.save(account));
    }
    @Override
    public AccountResponse findByNumber(String accountNo) {
        return mapper.toResponse(repository.findById(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo)));
    }
    @Override
    public List<AccountResponse> findByCif(String cifNo) {
        return repository.findByCifNo(cifNo).stream().map(mapper::toResponse).toList();
    }
}
