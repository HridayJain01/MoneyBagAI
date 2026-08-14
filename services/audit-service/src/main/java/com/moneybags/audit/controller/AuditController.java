package com.moneybags.audit.controller;

import com.moneybags.audit.api.ApiModels.*;
import com.moneybags.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Producer path. Accepted with 202 and never blocking: an audit failure must not roll
     * back a completed banking transaction.
     */
    @Operation(summary = "Append an audit event")
    @PostMapping("/internal/v1/audit-events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestResult append(@RequestHeader(value = "X-Service-Name", required = false) String serviceName,
                               @RequestBody AuditEventRequest request) {
        return auditService.append(serviceName == null ? "unknown" : serviceName, request);
    }

    @Operation(summary = "Search audit events")
    @GetMapping("/api/v1/audit-events")
    public PageResponse<AuditEventDetail> search(
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) String actorEmployeeId,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditService.search(sourceService, eventType, aggregateType, aggregateId,
                actorEmployeeId, branchCode, from, to, page, size);
    }

    @GetMapping("/api/v1/audit-events/{eventId}")
    public ResponseEntity<AuditEventDetail> detail(@PathVariable String eventId) {
        AuditEventDetail detail = auditService.detail(eventId);
        return detail == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(detail);
    }

    /** Follows one request across every service that handled it. */
    @Operation(summary = "Trace a request across services by correlation id")
    @GetMapping("/api/v1/audit-events/trace/{correlationId}")
    public List<AuditEventDetail> trace(@PathVariable String correlationId) {
        return auditService.trace(correlationId);
    }

    @GetMapping("/api/v1/audit-events/accounts/{accountId}")
    public List<AuditEventDetail> forAccount(@PathVariable String accountId) {
        return auditService.forAggregate("ACCOUNT", accountId);
    }

    @GetMapping("/api/v1/audit-events/transactions/{transactionId}")
    public List<AuditEventDetail> forTransaction(@PathVariable String transactionId) {
        return auditService.forAggregate("TRANSACTION", transactionId);
    }
}
