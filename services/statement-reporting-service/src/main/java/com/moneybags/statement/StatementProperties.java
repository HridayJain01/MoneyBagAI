package com.moneybags.statement;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("moneybags.statement")
public class StatementProperties {
    private String storageDirectory = "./data/statements";
    private String downloadSecret;
    private long downloadLinkMinutes = 10;
    private long fileRetentionDays = 30;
    private long workerDelayMs = 2000;
    private boolean workerEnabled = true;
}
