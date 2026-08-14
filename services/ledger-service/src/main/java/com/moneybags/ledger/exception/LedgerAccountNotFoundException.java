package com.moneybags.ledger.exception;

import org.springframework.http.HttpStatus;

public class LedgerAccountNotFoundException extends LedgerException {
    public LedgerAccountNotFoundException(String code) {
        super(HttpStatus.NOT_FOUND, "LEDGER_ACCOUNT_NOT_FOUND", "Ledger account not found: " + code);
    }
}
