package com.moneybags.ledger.entity;

import com.moneybags.ledger.enums.EntrySide;
import com.moneybags.ledger.enums.LedgerAccountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerAccountBalanceTest {
    @Test
    void appliesNormalSideAccountingRules() {
        LedgerAccount asset = LedgerAccount.create("110100", "Cash", LedgerAccountType.ASSET,
                EntrySide.DEBIT, "USD");
        LedgerAccount income = LedgerAccount.create("410100", "Fee Income", LedgerAccountType.INCOME,
                EntrySide.CREDIT, "USD");

        asset.apply(EntrySide.DEBIT, new BigDecimal("500.00"));
        asset.apply(EntrySide.CREDIT, new BigDecimal("40.00"));
        income.apply(EntrySide.CREDIT, new BigDecimal("2.00"));
        income.apply(EntrySide.DEBIT, new BigDecimal("0.50"));

        assertThat(asset.getBalance()).isEqualByComparingTo("460.00");
        assertThat(income.getBalance()).isEqualByComparingTo("1.50");
    }
}
