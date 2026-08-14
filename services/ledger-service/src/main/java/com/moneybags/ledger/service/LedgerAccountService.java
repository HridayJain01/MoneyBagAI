package com.moneybags.ledger.service;

import com.moneybags.ledger.dto.LedgerAccountResponse;
import com.moneybags.ledger.dto.LedgerBalanceResponse;
import com.moneybags.ledger.entity.LedgerAccount;
import com.moneybags.ledger.exception.LedgerAccountNotFoundException;
import com.moneybags.ledger.mapper.LedgerMapper;
import com.moneybags.ledger.repository.LedgerAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerAccountService {
    private final LedgerAccountRepository repository;
    private final LedgerMapper mapper;

    public List<LedgerAccountResponse> findAll() {
        return repository.findAllByOrderByCodeAsc().stream().map(mapper::toAccountResponse).toList();
    }

    public LedgerAccountResponse findByCode(String code) {
        return mapper.toAccountResponse(get(code));
    }

    public LedgerBalanceResponse balance(String code) {
        return mapper.toBalanceResponse(get(code));
    }

    private LedgerAccount get(String code) {
        return repository.findByCode(code.trim())
                .orElseThrow(() -> new LedgerAccountNotFoundException(code));
    }
}
