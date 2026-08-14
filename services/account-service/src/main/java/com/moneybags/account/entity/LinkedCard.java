package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Backs transaction-service's card-service contract. A card is a secondary product
 * linked to an account, so it belongs here rather than in a separate module.
 */
@Entity
@Table(name = "linked_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkedCard {

    @Id
    @Column(name = "card_id", length = 36)
    private String cardId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "cif_no", nullable = false, length = 30)
    private String cifNo;

    @Column(name = "masked_pan", nullable = false, length = 19)
    private String maskedPan;

    @Column(name = "card_type", nullable = false, length = 16)
    private String cardType;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;
}
