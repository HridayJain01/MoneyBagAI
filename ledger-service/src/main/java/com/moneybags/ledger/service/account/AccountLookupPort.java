package com.moneybags.ledger.service.account;

public interface AccountLookupPort {
    AccountSummary findByAccountId(Long accountId);

    record AccountSummary(Long accountId, Long customerId, String currencyCode, String status) {}
}
