package com.moneybags.transaction.entity;

import com.moneybags.transaction.domain.FinancialEnums.IdempotencyState;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "transaction_idempotency_records")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IdempotencyRecord {
    @Id @Column(name = "idempotency_id", length = 36) private String id;
    @Column(name = "caller_scope", nullable = false, length = 128) private String callerScope;
    @Column(nullable = false, length = 80) private String operation;
    @Column(name = "idempotency_key", nullable = false, length = 128) private String key;
    @Column(name = "request_hash", nullable = false, length = 64, columnDefinition = "char(64)") private String requestHash;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transaction_id") private Transaction transaction;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private IdempotencyState state;
    @Column(name = "response_code") private Integer responseCode;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;
    @PrePersist void create(){ if(id==null)id=UUID.randomUUID().toString(); if(createdAt==null)createdAt=Instant.now(); }
}
