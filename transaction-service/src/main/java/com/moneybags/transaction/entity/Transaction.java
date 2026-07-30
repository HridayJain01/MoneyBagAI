package com.moneybags.transaction.entity;

import com.moneybags.transaction.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_request_ref", columnNames = "request_ref")
}, indexes = @Index(name = "idx_transactions_account_date", columnList = "account_no,txn_date"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Long txnId;
    @Column(name = "txn_ref", nullable = false, length = 50)
    private String txnRef;
    @Column(name = "request_ref", nullable = false, length = 80)
    private String requestRef;
    @Column(name = "account_no", nullable = false, length = 30)
    private String accountNo;
    @Enumerated(EnumType.STRING) @Column(name = "txn_type", nullable = false, length = 30)
    private TransactionType txnType;
    @Enumerated(EnumType.STRING) @Column(name = "dr_cr", nullable = false, length = 5)
    private DebitCredit drCr;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "running_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal runningBalance;
    @Column(name = "counterparty_acct", length = 30)
    private String counterpartyAcct;
    @Column(length = 500)
    private String narration;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TransactionStatus status;
    @Column(name = "txn_date", nullable = false)
    private LocalDateTime txnDate;
    @Column(name = "posted_by", nullable = false, length = 80)
    private String postedBy;
}
