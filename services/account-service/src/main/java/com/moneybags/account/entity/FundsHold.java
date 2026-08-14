package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "funds_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundsHold {

    @Id
    @Column(name = "hold_id", length = 36)
    private String holdId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    /** Null for manual and lien holds. Unique when present: one hold per transaction. */
    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(nullable = false, length = 64)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "hold_type", nullable = false, length = 16)
    private HoldType holdType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HoldStatus status;

    @Column(name = "placed_by", nullable = false, length = 64)
    private String placedBy;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "release_reason", length = 120)
    private String releaseReason;
}
