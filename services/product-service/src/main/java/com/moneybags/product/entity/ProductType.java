package com.moneybags.product.entity;

public enum ProductType {
    SAVINGS,
    CURRENT,
    TERM_DEPOSIT,
    RECURRING_DEPOSIT;

    /** FD and RD carry a tenure and must be funded at opening. */
    public boolean isTermBased() {
        return this == TERM_DEPOSIT || this == RECURRING_DEPOSIT;
    }
}
