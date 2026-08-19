package com.moneybags.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Append-only. Nothing in this service updates or deletes a row. */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    /** Supplied by the producer; doubles as the idempotency key. */
    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "source_service", nullable = false, length = 48)
    private String sourceService;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "aggregate_type", length = 48)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 64)
    private String aggregateId;

    @Column(name = "actor_user_id", length = 64)
    private String actorUserId;

    @Column(name = "actor_employee_id", length = 64)
    private String actorEmployeeId;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "http_path", length = 255)
    private String httpPath;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Lob
    @Column
    private String payload;
}
