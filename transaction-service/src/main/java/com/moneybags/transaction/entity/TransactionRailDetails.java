package com.moneybags.transaction.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="transaction_rail_details")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TransactionRailDetails {
    @Id @Column(name="transaction_id",length=36) private String transactionId;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @MapsId @JoinColumn(name="transaction_id") private Transaction transaction;
    @Column(name="upi_address",length=160) private String upiAddress;
    @Column(name="cheque_number",length=64) private String chequeNumber;
    @Column(name="card_id",length=128) private String cardId;
    @Column(name="client_reference",length=128) private String clientReference;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @PrePersist void create(){if(createdAt==null)createdAt=Instant.now();}
}
