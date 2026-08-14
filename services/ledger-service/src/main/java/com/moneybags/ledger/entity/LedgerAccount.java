package com.moneybags.ledger.entity;

import com.moneybags.ledger.enums.EntrySide;
import com.moneybags.ledger.enums.LedgerAccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_accounts")
@Getter
@NoArgsConstructor
public class LedgerAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 24)
    private LedgerAccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_side", nullable = false, length = 8)
    private EntrySide normalSide;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static LedgerAccount create(String code, String name, LedgerAccountType accountType,
                                       EntrySide normalSide, String currencyCode) {
        LedgerAccount account = new LedgerAccount();
        account.code = code;
        account.name = name;
        account.accountType = accountType;
        account.normalSide = normalSide;
        account.balance = BigDecimal.ZERO;
        account.currencyCode = currencyCode;
        account.active = true;
        account.createdAt = Instant.now();
        account.updatedAt = account.createdAt;
        return account;
    }

    public void apply(EntrySide side, BigDecimal amount) {
        boolean increases = side == normalSide;
        balance = increases ? balance.add(amount) : balance.subtract(amount);
        updatedAt = Instant.now();
    }
}
