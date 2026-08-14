package com.moneybags.account.entity;

public enum HoldType {
    /** Placed by transaction-service while a transaction is in flight. */
    TRANSACTION,
    /** Legal or regulatory hold placed by staff. */
    LIEN,
    /** Operational hold placed by staff. */
    MANUAL
}
