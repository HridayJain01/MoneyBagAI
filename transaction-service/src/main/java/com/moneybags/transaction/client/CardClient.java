package com.moneybags.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="card-service")
public interface CardClient {
    @GetMapping("/internal/v1/cards/{cardId}/payment-context")
    CardContext context(@PathVariable String cardId,@RequestParam String accountHolderId);
    record CardContext(String cardId,String accountHolderId,String linkedAccountId,String status,String currency){}
}
