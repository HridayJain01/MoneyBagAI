package com.moneybags.ledger.exception;

import org.springframework.http.HttpStatus;

public class AccountLookupException extends LedgerException {
    public AccountLookupException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "CUSTOMER_ACCOUNT_LOOKUP_FAILED", message);
    }
}
