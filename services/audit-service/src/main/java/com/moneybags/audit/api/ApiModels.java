package com.moneybags.audit.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {
    }

    /**
     * Producer contract. Everything except the event body is optional so a producer can
     * send what it knows -- a gateway filter has HTTP context but no aggregate, an outbox
     * publisher has the reverse.
     */
    public record AuditEventRequest(
            String eventId,
            String sourceService,
            String eventType,
            String aggregateType,
            String aggregateId,
            String actorUserId,
            String actorEmployeeId,
            String branchCode,
            String correlationId,
            String httpMethod,
            String httpPath,
            Integer httpStatus,
            Instant occurredAt,
            Map<String, Object> payload) {
    }

    /** result is APPLIED, DUPLICATE or PARKED. */
    public record IngestResult(String eventId, String result) {
    }

    public record AuditEventDetail(
            String eventId,
            String sourceService,
            String eventType,
            String aggregateType,
            String aggregateId,
            String actorUserId,
            String actorEmployeeId,
            String branchCode,
            String correlationId,
            String httpMethod,
            String httpPath,
            Integer httpStatus,
            Instant occurredAt,
            Instant ingestedAt,
            String payload) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }
}
