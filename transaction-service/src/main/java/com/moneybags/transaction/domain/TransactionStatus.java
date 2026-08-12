package com.moneybags.transaction.domain;

public enum TransactionStatus {
    RECEIVED, VALIDATED, PENDING_APPROVAL, APPROVED, FUNDS_RESERVED,
    PROCESSING, PROJECTION_PENDING, SETTLED, COMPLETED,
    FAILED, REJECTED, CANCELLED, REVERSAL_PENDING, REVERSED;

    public boolean terminal() {
        return this == COMPLETED || this == FAILED || this == REJECTED || this == CANCELLED || this == REVERSED;
    }
}
