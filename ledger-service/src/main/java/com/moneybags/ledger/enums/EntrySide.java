package com.moneybags.ledger.enums;

public enum EntrySide {
    DEBIT,
    CREDIT;

    public EntrySide opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
