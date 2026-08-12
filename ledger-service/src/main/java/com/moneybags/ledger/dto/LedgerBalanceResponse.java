package com.moneybags.ledger.dto;

import com.moneybags.ledger.enums.EntrySide;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerBalanceResponse(
        String code,
        BigDecimal balance,
        String currencyCode,
        EntrySide normalSide,
        Instant asOf
) {}
