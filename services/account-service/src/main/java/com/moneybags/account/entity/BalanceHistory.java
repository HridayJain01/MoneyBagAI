package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "balance_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Column(name = "transaction_reference", length = 64)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "ledger_balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal ledgerBalanceBefore;

    @Column(name = "ledger_balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal ledgerBalanceAfter;

    @Column(name = "held_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal heldBefore;

    @Column(name = "held_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal heldAfter;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
