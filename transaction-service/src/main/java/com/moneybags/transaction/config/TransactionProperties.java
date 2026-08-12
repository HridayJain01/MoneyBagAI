package com.moneybags.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "moneybags.transaction")
public class TransactionProperties {
    private String defaultCurrency = "INR";
    private int miniStatementSize = 10;
    private final Ledger ledger = new Ledger();
    private final Outbox outbox = new Outbox();

    @Getter @Setter
    public static class Ledger {
        private String cashAsset = "110100";
        private String customerDepositControl = "210000";
        private String internalClearing = "220100";
        private String externalClearing = "220200";
        private String feeIncome = "410100";
    }

    @Getter @Setter
    public static class Outbox {
        private boolean enabled = true;
        private int batchSize = 50;
        private int maxAttempts = 10;
    }
}
