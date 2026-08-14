package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Dedupe record for applied projections.
 *
 * <p>Keyed on {@code eventId} (logical replay) with a UNIQUE {@code dedupKey} (a
 * retried HTTP call). Both are needed: transaction-service's publisher retries the same
 * event up to ten times, and a reversal emits a fresh eventId for a genuinely new effect.
 */
@Entity
@Table(name = "projection_inbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectionInbox {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "dedup_key", nullable = false, length = 160)
    private String dedupKey;

    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(name = "event_type", length = 64)
    private String eventType;

    @Column(name = "hold_id", length = 36)
    private String holdId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(nullable = false, length = 16)
    private String outcome;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;
}
