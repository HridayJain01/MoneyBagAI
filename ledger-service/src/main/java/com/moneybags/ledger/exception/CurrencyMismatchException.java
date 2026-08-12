package com.moneybags.ledger.exception;

import org.springframework.http.HttpStatus;

public class CurrencyMismatchException extends LedgerException {
    public CurrencyMismatchException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "CURRENCY_MISMATCH", message);
    }
}
