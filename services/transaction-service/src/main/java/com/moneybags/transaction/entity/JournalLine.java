package com.moneybags.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "transaction_journal_lines")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalLine {
    @Id @Column(name = "journal_line_id", length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "journal_id") private JournalEntry journal;
    @Column(name = "line_no", nullable = false) private int lineNo;
    @Column(name = "ledger_account_code", nullable = false, length = 32) private String ledgerAccountCode;
    @Column(name = "account_id", length = 64) private String accountId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal debit;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal credit;
    @Column(nullable = false, length = 255) private String description;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void create(){ if(id==null)id=UUID.randomUUID().toString(); if(createdAt==null)createdAt=Instant.now(); if(debit==null)debit=BigDecimal.ZERO; if(credit==null)credit=BigDecimal.ZERO; }
}
