package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_id", length = 36)
    private String idempotencyId;

    /**
     * transaction-service sends NO actor headers to this service (it has no Feign
     * RequestInterceptor), so internal callers fall back to a constant scope rather
     * than dereferencing an absent employee id.
     */
    @Column(name = "caller_scope", nullable = false, length = 128)
    private String callerScope;

    @Column(nullable = false, length = 80)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "resource_id", length = 36)
    private String resourceId;

    @Column(nullable = false, length = 20)
    private String state;

    @Column(name = "response_code")
    private Integer responseCode;

    @Lob
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
