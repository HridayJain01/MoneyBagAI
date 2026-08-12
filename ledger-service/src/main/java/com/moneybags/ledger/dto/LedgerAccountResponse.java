package com.moneybags.ledger.dto;

import com.moneybags.ledger.enums.EntrySide;
import com.moneybags.ledger.enums.LedgerAccountType;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerAccountResponse(
        String code,
        String name,
        LedgerAccountType type,
        EntrySide normalSide,
        BigDecimal balance,
        String currencyCode,
        boolean active,
        Instant updatedAt,
        long version
) {}
