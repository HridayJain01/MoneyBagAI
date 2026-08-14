package com.moneybags.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerClient {

    /** Product-opening eligibility. A non-2xx means the CIF cannot open an account. */
    @GetMapping("/api/v1/customers/{cif}/eligibility")
    EligibilityResponse eligibility(@PathVariable("cif") String cif);

    /**
     * Deliberately lenient: customer-service's eligibility payload has grown over time,
     * and account-service only needs to know whether opening is permitted. Unknown
     * fields are ignored by Spring Boot's Jackson defaults.
     */
    record EligibilityResponse(boolean eligible, String kycStatus, String status, String reason) {
    }
}
