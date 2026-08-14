package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLimits {

    @Id
    @Column(name = "account_id", length = 36)
    private String accountId;

    @Column(name = "per_transaction_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal perTransactionLimit;

    @Column(name = "daily_withdrawal_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyWithdrawalLimit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
