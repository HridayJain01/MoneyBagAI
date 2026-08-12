package com.moneybags.ledger.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class UnbalancedJournalException extends LedgerException {
    public UnbalancedJournalException(BigDecimal debit, BigDecimal credit) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "UNBALANCED_JOURNAL",
                "Journal is unbalanced: debit=" + debit + ", credit=" + credit);
    }
}
