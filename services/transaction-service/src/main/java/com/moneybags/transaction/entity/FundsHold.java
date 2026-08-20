package com.moneybags.transaction.entity;

import com.moneybags.transaction.domain.FinancialEnums.HoldStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "transaction_funds_holds")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FundsHold {
    @Id @Column(name = "hold_id", length = 36) private String id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transaction_id") private Transaction transaction;
    @Column(name = "account_id", nullable = false, length = 64) private String accountId;
    @Column(name = "external_hold_id", nullable = false, unique = true, length = 128) private String externalHoldId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private HoldStatus status;
    @Column(name = "operation_key", nullable = false, unique = true, length = 128) private String operationKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void create() { if (id == null) id = UUID.randomUUID().toString(); Instant now=Instant.now(); if(createdAt==null)createdAt=now; updatedAt=now; }
    @PreUpdate void update() { updatedAt=Instant.now(); }
}
