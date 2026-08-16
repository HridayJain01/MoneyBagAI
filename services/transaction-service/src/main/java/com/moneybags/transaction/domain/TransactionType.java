package com.moneybags.transaction.domain;

public enum TransactionType {
    DEPOSIT, WITHDRAWAL, INTERNAL_TRANSFER, NEFT, RTGS, IMPS, UPI, CHEQUE,
    CARD_PAYMENT, PRODUCT_PURCHASE, REVERSAL;

    public boolean debitsAccount() {
        return this != DEPOSIT && this != CHEQUE;
    }

    public boolean externallyCleared() {
        return this == NEFT || this == RTGS || this == IMPS || this == UPI || this == CHEQUE || this == CARD_PAYMENT;
    }
}
