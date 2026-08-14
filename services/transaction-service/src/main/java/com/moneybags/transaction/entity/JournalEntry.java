package com.moneybags.transaction.entity;

import com.moneybags.transaction.domain.FinancialEnums.JournalStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name = "journal_entries")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntry {
    @Id @Column(name = "journal_id", length = 36) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transaction_id") private Transaction transaction;
    @Column(name = "journal_reference", nullable = false, unique = true, length = 64) private String reference;
    @Column(name = "journal_type", nullable = false, length = 32) private String type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private JournalStatus status;
    @Column(name = "total_debit", nullable = false, precision = 19, scale = 4) private BigDecimal totalDebit;
    @Column(name = "total_credit", nullable = false, precision = 19, scale = 4) private BigDecimal totalCredit;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "posted_at") private Instant postedAt;
    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("lineNo")
    @Builder.Default private List<JournalLine> lines = new ArrayList<>();
    @PrePersist void create(){ if(id==null)id=UUID.randomUUID().toString(); if(createdAt==null)createdAt=Instant.now(); }
    public void addLine(JournalLine line) { line.setJournal(this); lines.add(line); }
}
