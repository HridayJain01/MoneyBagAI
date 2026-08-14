package com.moneybags.ledger.exception;

import org.springframework.http.HttpStatus;

public class InvalidJournalException extends LedgerException {
    public InvalidJournalException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_JOURNAL", message);
    }
}
