package com.moneybags.ledger.dto;

import com.moneybags.ledger.enums.EntrySide;
import com.moneybags.ledger.enums.JournalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerLedgerEntryResponse(
        Long journalId,
        String journalReference,
        Long transactionId,
        JournalStatus journalStatus,
        int lineNumber,
        String ledgerCode,
        Long customerAccountId,
        EntrySide side,
        BigDecimal amount,
        String currencyCode,
        String description,
        Instant postedAt
) {}
