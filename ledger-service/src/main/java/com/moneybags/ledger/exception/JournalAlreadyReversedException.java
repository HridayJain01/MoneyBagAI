package com.moneybags.ledger.exception;

import org.springframework.http.HttpStatus;

public class JournalAlreadyReversedException extends LedgerException {
    public JournalAlreadyReversedException(Long journalId) {
        super(HttpStatus.CONFLICT, "JOURNAL_ALREADY_REVERSED", "Journal has already been reversed: " + journalId);
    }
}
