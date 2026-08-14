package com.moneybags.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.Instant;
import java.util.Map;

@FeignClient(name = "audit-service")
public interface AuditClient {

    @PostMapping("/internal/v1/audit-events")
    void append(@RequestHeader("X-Service-Name") String serviceName,
                @RequestBody AuditEvent event);

    record AuditEvent(
            String eventId,
            String sourceService,
            String eventType,
            String aggregateType,
            String aggregateId,
            String actorEmployeeId,
            String branchCode,
            String correlationId,
            Instant occurredAt,
            Map<String, Object> payload) {
    }
}
