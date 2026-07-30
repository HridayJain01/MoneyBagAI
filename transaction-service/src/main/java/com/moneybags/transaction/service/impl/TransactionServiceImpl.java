package com.moneybags.transaction.service.impl;

import com.moneybags.transaction.client.AccountClient;
import com.moneybags.transaction.dto.*;
import com.moneybags.transaction.entity.Transaction;
import com.moneybags.transaction.enums.*;
import com.moneybags.transaction.exception.ResourceNotFoundException;
import com.moneybags.transaction.mapper.TransactionMapper;
import com.moneybags.transaction.repository.TransactionRepository;
import com.moneybags.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository repository;
    private final TransactionMapper mapper;
    private final AccountClient accountClient;

    @Override @Transactional
    public TransactionResponse post(TransactionRequest request) {
        return repository.findByRequestRef(request.requestRef()).map(mapper::toResponse).orElseGet(() -> {
            AccountClient.AccountSummary account = accountClient.findAccount(request.accountNo());
            BigDecimal signedAmount = request.drCr() == DebitCredit.CR ? request.amount() : request.amount().negate();
            Transaction transaction = mapper.toEntity(request);
            transaction.setTxnRef(UUID.randomUUID().toString());
            transaction.setRunningBalance(account.balance().add(signedAmount));
            transaction.setStatus(TransactionStatus.POSTED);
            transaction.setTxnDate(LocalDateTime.now());
            // TODO atomically update account-service and add compensating/reversal behavior for distributed failures.
            return mapper.toResponse(repository.save(transaction));
        });
    }
    @Override
    public TransactionResponse findById(Long txnId) {
        return mapper.toResponse(repository.findById(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + txnId)));
    }
    @Override
    public List<TransactionResponse> findForAccount(String accountNo, LocalDateTime from, LocalDateTime to) {
        return repository.findByAccountNoAndTxnDateBetweenOrderByTxnDateDesc(accountNo, from, to)
                .stream().map(mapper::toResponse).toList();
    }
}
