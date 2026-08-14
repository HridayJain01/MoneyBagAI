package com.moneybags.branch_employee_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * identity-service registers with Eureka as {@code security-service}, not as its module
 * name. customer-service resolves the same id.
 */
@FeignClient(name = "security-service")
public interface SecurityClient {

    /** Existence probe. A non-2xx means the user does not exist. */
    @GetMapping("/api/v1/users/{userId}")
    UserSummary findUser(@PathVariable("userId") Long userId);

    /**
     * Pushes employment onto the identity user so the gateway can resolve employee and
     * branch from a session in a single call, instead of chaining identity ->
     * branch-employee on every request.
     */
    @PutMapping("/internal/v1/users/{userId}/employment")
    void setEmployment(@PathVariable("userId") Long userId, @RequestBody EmploymentRequest request);

    record UserSummary(Long userId, String username, String status) {
    }

    record EmploymentRequest(String employeeId, String branchCode) {
    }
}
