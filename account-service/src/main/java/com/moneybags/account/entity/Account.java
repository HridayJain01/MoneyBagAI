package com.moneybags.account.entity;

import com.moneybags.account.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "accounts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Account {
    @Id @Column(name = "account_no", length = 30)
    private String accountNo;
    @Column(name = "cif_no", nullable = false, length = 30)
    private String cifNo;
    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;
    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
    @Column(name = "min_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal minBalance;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AccountStatus status;
    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;
    @Column(name = "closed_on")
    private LocalDate closedOn;
    @Version
    @Column(nullable = false)
    private Integer version;
}
