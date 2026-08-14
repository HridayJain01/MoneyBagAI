package com.moneybags.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.audit.api.ApiModels.*;
import com.moneybags.audit.entity.AuditEvent;
import com.moneybags.audit.entity.AuditIngestFailure;
import com.moneybags.audit.repository.AuditEventRepository;
import com.moneybags.audit.repository.AuditIngestFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository events;
    private final AuditIngestFailureRepository failures;
    private final ObjectMapper objectMapper;

    /**
     * Idempotent on eventId: producers deliver from an outbox with retries, so the same
     * event will arrive more than once and must record only once.
     *
     * <p>A malformed payload is parked in audit_ingest_failures rather than rejected,
     * because losing an audit record silently is worse than keeping one that needs a
     * human to look at it.
     */
    @Transactional
    public IngestResult append(String sourceService, AuditEventRequest request) {
        String eventId = request.eventId() == null
                ? UUID.randomUUID().toString()
                : request.eventId();

        if (events.existsById(eventId)) {
            return new IngestResult(eventId, "DUPLICATE");
        }

        try {
            events.save(AuditEvent.builder()
                    .eventId(eventId)
                    .sourceService(request.sourceService() == null ? sourceService : request.sourceService())
                    .eventType(request.eventType() == null ? "UNKNOWN" : request.eventType())
                    .aggregateType(request.aggregateType())
                    .aggregateId(request.aggregateId())
                    .actorUserId(request.actorUserId())
                    .actorEmployeeId(request.actorEmployeeId())
                    .branchCode(request.branchCode())
                    .correlationId(request.correlationId())
                    .httpMethod(request.httpMethod())
                    .httpPath(truncate(request.httpPath(), 255))
                    .httpStatus(request.httpStatus())
                    .occurredAt(request.occurredAt() == null ? Instant.now() : request.occurredAt())
                    .ingestedAt(Instant.now())
                    .payload(request.payload() == null ? null : objectMapper.writeValueAsString(request.payload()))
                    .build());
            return new IngestResult(eventId, "APPLIED");
        } catch (Exception ex) {
            log.warn("Parking unprocessable audit event from {}: {}", sourceService, ex.getMessage());
            failures.save(AuditIngestFailure.builder()
                    .sourceService(sourceService)
                    .rawPayload(String.valueOf(request))
                    .failureReason(truncate(ex.getMessage(), 500))
                    .receivedAt(Instant.now())
                    .build());
            return new IngestResult(eventId, "PARKED");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDetail> search(String sourceService, String eventType,
                                                 String aggregateType, String aggregateId,
                                                 String actorEmployeeId, String branchCode,
                                                 Instant from, Instant to, int page, int size) {
        Page<AuditEvent> result = events.search(blankToNull(sourceService), blankToNull(eventType),
                blankToNull(aggregateType), blankToNull(aggregateId), blankToNull(actorEmployeeId),
                blankToNull(branchCode), from, to, PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::toDetail).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AuditEventDetail detail(String eventId) {
        return events.findById(eventId).map(this::toDetail).orElse(null);
    }

    /** Traces one request across every service that touched it. */
    @Transactional(readOnly = true)
    public List<AuditEventDetail> trace(String correlationId) {
        return events.findByCorrelationIdOrderByOccurredAtAsc(correlationId).stream()
                .map(this::toDetail).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventDetail> forAggregate(String aggregateType, String aggregateId) {
        return events.findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(aggregateType, aggregateId)
                .stream().map(this::toDetail).toList();
    }

    private AuditEventDetail toDetail(AuditEvent event) {
        return new AuditEventDetail(event.getEventId(), event.getSourceService(), event.getEventType(),
                event.getAggregateType(), event.getAggregateId(), event.getActorUserId(),
                event.getActorEmployeeId(), event.getBranchCode(), event.getCorrelationId(),
                event.getHttpMethod(), event.getHttpPath(), event.getHttpStatus(),
                event.getOccurredAt(), event.getIngestedAt(), event.getPayload());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
