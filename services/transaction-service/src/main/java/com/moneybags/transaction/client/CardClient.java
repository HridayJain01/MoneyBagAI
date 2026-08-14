package com.moneybags.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * There is no separate card service. A card is a secondary product linked to an account,
 * so account-service serves this contract; SERVICE_SCHEMA_DIVISION.md rules out a
 * standalone Card module. The url is explicit rather than relying on a discovery alias so
 * the indirection is visible to anyone reading this file.
 */
@FeignClient(name="card-service", url="${moneybags.card-service-url:http://localhost:8083}")
public interface CardClient {
    @GetMapping("/internal/v1/cards/{cardId}/payment-context")
    CardContext context(@PathVariable String cardId,@RequestParam String accountHolderId);
    record CardContext(String cardId,String accountHolderId,String linkedAccountId,String status,String currency){}
}
