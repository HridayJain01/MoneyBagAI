package com.moneybags.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;

@FeignClient(name = "transaction-service")
public interface TransactionClient {

    @PostMapping("/internal/v1/transactions/opening-deposits")
    void createOpeningDeposit(@RequestHeader("X-Service-Name") String serviceName,
                              @RequestHeader("Idempotency-Key") String idempotencyKey,
                              @RequestBody OpeningDepositCommand command);

    record OpeningDepositCommand(
            String accountId,
            BigDecimal amount,
            String currency,
            String applicationReference,
            String initiatedByEmployeeId,
            String branchCode,
            String correlationId) {
    }
}
