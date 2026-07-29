package com.moneybags.customer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "security-service")
public interface SecurityClient {
    @GetMapping("/api/v1/users/{userId}")
    UserSummary findUser(@PathVariable Long userId);

    record UserSummary(Long userId, String username, String status) {}
}
