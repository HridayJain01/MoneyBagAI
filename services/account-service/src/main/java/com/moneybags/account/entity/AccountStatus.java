package com.moneybags.account.entity;

/**
 * Account lifecycle.
 *
 * <p>{@link #ACTIVE} is the ONLY status for which transaction-service's
 * {@code transaction-context} reports "ACTIVE"; it pre-checks that string before it
 * will place a hold. The internal endpoints still re-check, because there is a window
 * between reading the context and placing the hold.
 */
public enum AccountStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    DORMANT,
    FROZEN,
    BLOCKED,
    CLOSURE_REQUESTED,
    MATURED,
    CLOSED;

    public boolean allowsDebit() {
        return this == ACTIVE;
    }

    /** A dormant account still accepts credits, and a credit wakes it up. */
    public boolean allowsCredit() {
        return this == ACTIVE || this == DORMANT;
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
