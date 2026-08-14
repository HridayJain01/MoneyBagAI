package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "account_holders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHolder {

    @Id
    @Column(name = "holder_id", length = 36)
    private String holderId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "cif_no", nullable = false, length = 30)
    private String cifNo;

    @Column(name = "holder_role", nullable = false, length = 16)
    private String holderRole;

    @Column(name = "holder_sequence", nullable = false)
    private Integer holderSequence;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @Column(name = "removed_at")
    private Instant removedAt;
}
