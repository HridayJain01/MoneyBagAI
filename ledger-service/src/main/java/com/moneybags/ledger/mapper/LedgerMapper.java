package com.moneybags.ledger.mapper;

import com.moneybags.ledger.dto.*;
import com.moneybags.ledger.entity.*;
import org.springframework.stereotype.Component;

@Component
public class LedgerMapper {
    public LedgerAccountResponse toAccountResponse(LedgerAccount account) {
        return new LedgerAccountResponse(account.getCode(), account.getName(), account.getAccountType(),
                account.getNormalSide(), account.getBalance(), account.getCurrencyCode(), account.isActive(),
                account.getUpdatedAt(), account.getVersion());
    }

    public LedgerBalanceResponse toBalanceResponse(LedgerAccount account) {
        return new LedgerBalanceResponse(account.getCode(), account.getBalance(), account.getCurrencyCode(),
                account.getNormalSide(), account.getUpdatedAt());
    }

    public JournalLineResponse toLineResponse(JournalLine line) {
        return new JournalLineResponse(line.getId(), line.getLineNumber(), line.getLedgerCode(),
                line.getLedgerAccount().getName(), line.getCustomerAccountId(), line.getSide(), line.getAmount(),
                line.getDescription(), line.getCreatedAt());
    }

    public JournalResponse toJournalResponse(JournalEntry journal) {
        return new JournalResponse(journal.getId(), journal.getJournalReference(), journal.getTransactionId(),
                journal.getJournalType(), journal.getDescription(), journal.getStatus(), journal.getCurrencyCode(),
                journal.getTotalDebit(), journal.getTotalCredit(), journal.getReversalOfJournalId(),
                journal.getCreatedAt(), journal.getPostedAt(), journal.getCreatedBy(),
                journal.immutableLines().stream().map(this::toLineResponse).toList());
    }

    public CustomerLedgerEntryResponse toCustomerEntryResponse(JournalLine line) {
        JournalEntry journal = line.getJournalEntry();
        return new CustomerLedgerEntryResponse(journal.getId(), journal.getJournalReference(),
                journal.getTransactionId(), journal.getStatus(), line.getLineNumber(), line.getLedgerCode(),
                line.getCustomerAccountId(), line.getSide(), line.getAmount(), journal.getCurrencyCode(),
                line.getDescription(), journal.getPostedAt());
    }
}
