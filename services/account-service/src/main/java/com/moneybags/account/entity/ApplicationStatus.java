package com.moneybags.account.entity;

public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isDecided() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
}
