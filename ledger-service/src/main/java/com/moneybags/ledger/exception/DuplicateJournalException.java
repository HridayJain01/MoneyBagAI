package com.moneybags.ledger.exception;

import org.springframework.http.HttpStatus;

public class DuplicateJournalException extends LedgerException {
    public DuplicateJournalException(String reference) {
        super(HttpStatus.CONFLICT, "DUPLICATE_JOURNAL_REFERENCE",
                "Journal reference already exists with different accounting details: " + reference);
    }
}
