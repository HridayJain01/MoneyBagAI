package com.moneybags.transaction.entity;

import com.moneybags.transaction.domain.FinancialEnums.ClearingStatus;
import com.moneybags.transaction.domain.PaymentRail;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Table(name = "clearing_instructions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ClearingInstruction {
    @Id @Column(name = "clearing_instruction_id", length = 36) private String id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transaction_id") private Transaction transaction;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private PaymentRail rail;
    @Column(name = "external_reference", length = 128) private String externalReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ClearingStatus status;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3, columnDefinition = "char(3)") private String currency;
    @Column(name = "settlement_date") private LocalDate settlementDate;
    @Column(name = "failure_reason", length = 500) private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void create(){ if(id==null)id=UUID.randomUUID().toString(); Instant now=Instant.now(); if(createdAt==null)createdAt=now; updatedAt=now; }
    @PreUpdate void update(){updatedAt=Instant.now();}
}
