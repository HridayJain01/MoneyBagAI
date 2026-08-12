package com.moneybags.transaction.entity;

import com.moneybags.transaction.domain.FinancialEnums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "transaction_legs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionLeg {
    @Id @Column(name = "leg_id", length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transaction_id") private Transaction transaction;
    @Column(name = "sequence_no", nullable = false) private int sequenceNo;
    @Enumerated(EnumType.STRING) @Column(name = "leg_role", nullable = false, length = 24) private LegRole role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Direction direction;
    @Column(name = "account_id", length = 64) private String accountId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Column(nullable = false, length = 255) private String description;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void create() { if (id == null) id = UUID.randomUUID().toString(); if (createdAt == null) createdAt = Instant.now(); }
}
