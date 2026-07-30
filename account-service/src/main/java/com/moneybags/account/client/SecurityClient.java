package com.moneybags.account.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name = "security-service")
public interface SecurityClient {
    @GetMapping("/api/v1/branches/{branchCode}")
    BranchSummary findBranch(@PathVariable String branchCode);
    record BranchSummary(String branchCode, String status) {}
}
