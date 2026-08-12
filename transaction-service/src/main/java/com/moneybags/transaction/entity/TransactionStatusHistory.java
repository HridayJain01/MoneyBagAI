package com.moneybags.transaction.entity;

import com.moneybags.transaction.domain.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "transaction_status_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionStatusHistory {
    @Id @Column(name = "history_id", length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transaction_id") private Transaction transaction;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 32) private TransactionStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 32) private TransactionStatus toStatus;
    @Column(name = "actor_id", nullable = false, length = 64) private String actorId;
    @Column(name = "actor_source", nullable = false, length = 32) private String actorSource;
    @Column(length = 500) private String reason;
    @Column(name = "correlation_id", nullable = false, length = 64) private String correlationId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @PrePersist void create(){ if(id==null)id=UUID.randomUUID().toString(); if(occurredAt==null)occurredAt=Instant.now(); }
}
