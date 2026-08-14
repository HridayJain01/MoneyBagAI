package com.moneybags.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_ingest_failures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditIngestFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_service", length = 48)
    private String sourceService;

    @Lob
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "failure_reason", nullable = false, length = 500)
    private String failureReason;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
