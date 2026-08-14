package com.moneybags.account.client;

import com.moneybags.account.api.InternalModels.AccountEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "statement-reporting-service")
public interface StatementClient {

    /**
     * The consumer requires {@code X-Service-Name} to be exactly "account-service" and
     * rejects anything else with SERVICE_AUTH_DENIED.
     */
    @PostMapping("/internal/v1/statement-read-model/accounts")
    IngestResult push(@RequestHeader("X-Service-Name") String serviceName,
                      @RequestBody AccountEvent event);

    /** result is "APPLIED" or "DUPLICATE"; both are HTTP 200 and both mean success. */
    record IngestResult(String sourceEventId, String result) {
    }
}
