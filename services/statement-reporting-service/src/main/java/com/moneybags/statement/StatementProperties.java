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
    private String accountServiceUrl = "http://localhost:8083";
    private String transactionServiceUrl = "http://localhost:8084";
    private int sourceConnectTimeoutMs = 1000;
    private int sourceReadTimeoutMs = 3000;
}
