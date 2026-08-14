package com.moneybags.ledger.exception;

import org.springframework.http.HttpStatus;

public class JournalNotFoundException extends LedgerException {
    public JournalNotFoundException(String identifier) {
        super(HttpStatus.NOT_FOUND, "JOURNAL_NOT_FOUND", "Journal not found: " + identifier);
    }
}
