package com.moneybags.ledger.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LedgerException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public LedgerException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
