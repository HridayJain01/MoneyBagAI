package com.moneybags.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@FeignClient(name="account-service")
public interface AccountClient {
    @GetMapping("/internal/v1/accounts/{accountId}/transaction-context")
    AccountContext context(@PathVariable String accountId,@RequestParam(required=false) String requestingCustomerId);
    @PostMapping("/internal/v1/accounts/{accountId}/holds")
    HoldResponse reserve(@PathVariable String accountId,@RequestHeader("Idempotency-Key") String key,@RequestBody HoldRequest request);
    @PostMapping("/internal/v1/accounts/{accountId}/holds/{holdId}/consume")
    void consume(@PathVariable String accountId,@PathVariable String holdId,@RequestHeader("Idempotency-Key") String key);
    @PostMapping("/internal/v1/accounts/{accountId}/holds/{holdId}/release")
    void release(@PathVariable String accountId,@PathVariable String holdId,@RequestHeader("Idempotency-Key") String key);
    @PostMapping("/internal/v1/account-projections")
    void project(@RequestHeader("Idempotency-Key") String key,@RequestBody ProjectionInstruction instruction);

    record AccountContext(String accountId,String customerId,String status,String currency,BigDecimal ledgerBalance,BigDecimal availableBalance,long version) {}
    record HoldRequest(String transactionId,BigDecimal amount,String currency,String reason) {}
    record HoldResponse(String holdId,String status,BigDecimal reservedAmount) {}
    record ProjectionInstruction(String eventId,String transactionId,String transactionReference,String accountId,String direction,BigDecimal amount,
                                 String currency,String holdId,String eventType,String correlationId) {}
}
