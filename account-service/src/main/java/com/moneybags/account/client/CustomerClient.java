package com.moneybags.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "customer-service")
public interface CustomerClient {
    @GetMapping("/api/v1/customers/{cifNo}")
    CustomerSummary findCustomer(@PathVariable Long cifNo);
    record CustomerSummary(Long cifNo, String status, String kycStatus) {}
}
