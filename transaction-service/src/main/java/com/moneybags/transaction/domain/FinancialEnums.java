package com.moneybags.transaction.domain;

public final class FinancialEnums {
    private FinancialEnums() {}
    public enum Direction { DEBIT, CREDIT }
    public enum LegRole { SOURCE, DESTINATION, FEE, COUNTERPARTY }
    public enum HoldStatus { FUNDS_HELD, CONSUMED, RELEASED }
    public enum JournalStatus { PENDING, POSTED }
    public enum ClearingStatus { CREATED, SUBMITTED, SETTLED, FAILED, CANCELLED }
    public enum OutboxStatus { PENDING, PUBLISHED, FAILED }
    public enum IdempotencyState { PROCESSING, COMPLETED, FAILED }
    public enum ReconciliationStatus { OPEN, ASSIGNED, RESOLVED }
}
