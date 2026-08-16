package com.moneybags.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moneybags.ledger")
public record LedgerProperties(
        String defaultCurrency,
        Accounts accounts
) {
    public LedgerProperties {
        defaultCurrency = defaultCurrency == null ? "INR" : defaultCurrency.toUpperCase();
        accounts = accounts == null ? new Accounts(
                "110100", "210000", "210100", "220100", "220200", "410100") : accounts;
    }

    public record Accounts(
            String cashAsset,
            String accountDepositControl,
            String termDepositControl,
            String internalClearing,
            String externalClearing,
            String feeIncome
    ) {}

}
