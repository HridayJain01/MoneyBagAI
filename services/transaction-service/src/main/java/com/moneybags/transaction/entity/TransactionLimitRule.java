package com.moneybags.transaction.entity;

import com.moneybags.transaction.domain.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "transaction_limit_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TransactionLimitRule {
    @Id @Column(name = "limit_rule_id", length = 36) private String id;
    @Enumerated(EnumType.STRING) @Column(name = "transaction_type", length = 32) private TransactionType type;
    @Enumerated(EnumType.STRING) @Column(length = 24) private PaymentRail rail;
    @Enumerated(EnumType.STRING) @Column(name = "payment_channel", length = 24) private PaymentChannel channel;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Column(name = "min_amount", precision = 19, scale = 4) private BigDecimal minAmount;
    @Column(name = "max_amount", precision = 19, scale = 4) private BigDecimal maxAmount;
    @Column(name = "daily_limit", precision = 19, scale = 4) private BigDecimal dailyLimit;
    @Column(name = "approval_threshold", precision = 19, scale = 4) private BigDecimal approvalThreshold;
    @Column(nullable = false) private int priority;
    @Column(nullable = false) private boolean active;
    @Column(name = "effective_from", nullable = false) private Instant effectiveFrom;
    @Column(name = "effective_to") private Instant effectiveTo;
}
