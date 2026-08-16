package com.moneybags.ledger.service.account;

import com.moneybags.ledger.client.AccountClient;
import com.moneybags.ledger.exception.AccountLookupException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountServiceLookupAdapter implements AccountLookupPort {
    private final AccountClient accountClient;

    @Override
    public AccountSummary findByAccountId(String accountId) {
        try {
            AccountClient.AccountContext account = accountClient.context(accountId);
            return new AccountSummary(account.accountId(), account.accountHolderId(),
                    account.currency(), account.status());
        } catch (Exception exception) {
            throw new AccountLookupException("Customer account lookup failed: " + accountId);
        }
    }
}
