package com.moneybags.account.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    void availableBalanceIncludesTheMinimumBalance() {
        Account account = Account.builder()
                .ledgerBalance(new BigDecimal("5000.00"))
                .heldAmount(BigDecimal.ZERO)
                .minBalance(new BigDecimal("1000.00"))
                .overdraftLimit(BigDecimal.ZERO)
                .build();

        assertThat(account.availableBalance()).isEqualByComparingTo("5000.00");
    }

    @Test
    void availableBalanceStillSubtractsHoldsAndIncludesOverdraft() {
        Account account = Account.builder()
                .ledgerBalance(new BigDecimal("5000.00"))
                .heldAmount(new BigDecimal("300.00"))
                .minBalance(new BigDecimal("1000.00"))
                .overdraftLimit(new BigDecimal("200.00"))
                .build();

        assertThat(account.availableBalance()).isEqualByComparingTo("4900.00");
    }
}
