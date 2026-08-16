package com.moneybags.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@FeignClient(name="account-service")
public interface AccountClient {
    @GetMapping("/internal/v1/accounts/{accountId}/transaction-context")
    AccountContext context(@PathVariable String accountId);
    @PostMapping("/internal/v1/accounts/{accountId}/holds")
    HoldResponse reserve(@PathVariable String accountId,@RequestHeader("Idempotency-Key") String key,@RequestBody HoldRequest request);
    @PostMapping("/internal/v1/accounts/{accountId}/holds/{holdId}/consume")
    void consume(@PathVariable String accountId,@PathVariable String holdId,@RequestHeader("Idempotency-Key") String key);
    @PostMapping("/internal/v1/accounts/{accountId}/holds/{holdId}/release")
    void release(@PathVariable String accountId,@PathVariable String holdId,@RequestHeader("Idempotency-Key") String key);
    @PostMapping("/internal/v1/account-projections")
    void project(@RequestHeader("Idempotency-Key") String key,@RequestBody ProjectionInstruction instruction);
    @PostMapping("/internal/v1/account-product-ownerships")
    OwnedProductResult projectOwnedProduct(@RequestHeader("X-Service-Name") String serviceName,
                                           @RequestBody OwnedProductProjection instruction);

    record AccountContext(String accountId,String accountHolderId,String status,String currency,BigDecimal ledgerBalance,BigDecimal availableBalance,long version) {}
    record HoldRequest(String transactionId,BigDecimal amount,String currency,String reason) {}
    record HoldResponse(String holdId,String status,BigDecimal reservedAmount) {}
    record ProjectionInstruction(String eventId,String transactionId,String transactionReference,String accountId,String direction,BigDecimal amount,
                                 String currency,String holdId,String eventType,String correlationId) {}
    record OwnedProductProjection(String ownershipId,String action,String purchaseTransactionId,String reversalTransactionId,
                                  String ownerAccountId,String productCode,String productName,String productType,
                                  Long productVersionId,Integer productVersionNumber,BigDecimal principalAmount,
                                  String currency,BigDecimal interestRate,Integer tenureMonths,
                                  LocalDate acquiredOn,LocalDate maturityDate) {}
    record OwnedProductResult(String ownershipId,String ownerAccountId,String productCode,String productName,
                              String productType,Long productVersionId,Integer productVersionNumber,
                              String acquisitionType,BigDecimal principalAmount,String currency,
                              BigDecimal interestRate,Integer tenureMonths,LocalDate acquiredOn,
                              LocalDate maturityDate,String status,String purchaseTransactionId,
                              String reversalTransactionId) {}
}
