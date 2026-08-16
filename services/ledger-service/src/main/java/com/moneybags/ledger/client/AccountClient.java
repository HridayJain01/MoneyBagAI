package com.moneybags.ledger.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountClient {
    @GetMapping("/internal/v1/accounts/{accountId}/transaction-context")
    AccountContext context(@PathVariable String accountId);

    record AccountContext(String accountId, String accountHolderId, String status, String currency,
                          BigDecimal ledgerBalance, BigDecimal availableBalance, long version) {}
}
