package com.moneybags.account.service;

import com.moneybags.account.config.AccountProperties;
import com.moneybags.account.entity.Account;
import com.moneybags.account.entity.HoldStatus;
import com.moneybags.account.repository.AccountRepository;
import com.moneybags.account.repository.FundsHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Cross-checks the incrementally maintained {@code accounts.held_amount} against the
 * sum of HELD rows in {@code funds_holds}.
 *
 * <p>Deliberately REPORTS drift rather than silently correcting it. A mismatch means a
 * hold path has a bug; quietly patching the number would destroy the only evidence that
 * it happened. Correction is an operator decision.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeldAmountReconciler {

    private final AccountRepository accounts;
    private final FundsHoldRepository holds;
    private final AccountProperties properties;

    @Scheduled(fixedDelayString = "${moneybags.account.reconciliation.fixed-delay-ms:60000}")
    @Transactional(readOnly = true)
    public void reconcile() {
        if (!properties.getReconciliation().isEnabled()) {
            return;
        }
        for (Account account : accounts.findAll()) {
            BigDecimal expected = holds.sumByAccountAndStatus(account.getAccountId(), HoldStatus.HELD);
            if (expected.compareTo(account.getHeldAmount()) != 0) {
                log.error("HELD AMOUNT DRIFT on account {}: column={} but sum(HELD holds)={}. "
                                + "Available balance is wrong until this is investigated.",
                        account.getAccountId(), account.getHeldAmount(), expected);
            }
        }
    }
}
