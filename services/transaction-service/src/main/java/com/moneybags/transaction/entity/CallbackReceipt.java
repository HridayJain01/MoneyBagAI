package com.moneybags.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "callback_receipts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CallbackReceipt {
    @Id @Column(name = "callback_receipt_id", length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transaction_id") private Transaction transaction;
    @Column(name = "callback_type", nullable = false, length = 32) private String callbackType;
    @Column(name = "provider_event_id", nullable = false, length = 128) private String providerEventId;
    @Column(name = "request_hash", nullable = false, length = 64, columnDefinition = "char(64)") private String requestHash;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    @PrePersist void create(){ if(id==null)id=UUID.randomUUID().toString(); if(processedAt==null)processedAt=Instant.now(); }
}
