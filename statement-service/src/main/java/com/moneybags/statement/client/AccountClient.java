package com.moneybags.statement.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@FeignClient(name = "account-service")
public interface AccountClient {
    @GetMapping("/api/v1/accounts/{accountNo}")
    AccountSummary findAccount(@PathVariable String accountNo);
    record AccountSummary(String accountNo, String cifNo, String productCode, String branchCode,
                          BigDecimal balance, String status) {}
}
