package com.moneybags.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @Column(name = "notification_id", length = 36)
    private String notificationId;

    /** The producer's Idempotency-Key. UNIQUE: prevents sending the same message twice. */
    @Column(name = "dedup_key", nullable = false, length = 160)
    private String dedupKey;

    @Column(nullable = false, length = 10)
    private String channel;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(name = "template_code", length = 60)
    private String templateCode;

    @Column(length = 255)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "source_service", length = 48)
    private String sourceService;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "cif_no", length = 30)
    private String cifNo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}
