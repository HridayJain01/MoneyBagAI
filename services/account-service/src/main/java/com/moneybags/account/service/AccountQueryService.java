package com.moneybags.account.service;

import com.moneybags.account.api.InternalModels.AccountContext;
import com.moneybags.account.api.InternalModels.CardContext;
import com.moneybags.account.api.InternalModels.StatementContext;
import com.moneybags.account.entity.Account;
import com.moneybags.account.entity.LinkedCard;
import com.moneybags.account.repository.AccountRepository;
import com.moneybags.account.repository.LinkedCardRepository;
import com.moneybags.account.support.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountQueryService {

    private final AccountRepository accounts;
    private final LinkedCardRepository cards;

    /**
     * Serves transaction-service's pre-flight check.
     *
     * <p>Built from an explicit record rather than the entity so the wire shape cannot
     * drift as the entity grows. Every field is populated unconditionally: availableBalance
     * is dereferenced without a null check by the caller, and version binds to a
     * primitive long.
     */
    @Transactional(readOnly = true)
    public AccountContext transactionContext(String accountId) {
        Account account = require(accountId);
        return new AccountContext(
                account.getAccountId(),
                account.getCifNo(),
                account.getStatus().name(),
                account.getCurrency(),
                account.getLedgerBalance(),
                account.availableBalance(),
                account.getVersion());
    }

    /**
     * Serves statement-reporting-service's fallback read. customerId and branchId drive
     * its scope enforcement, so they must be the real values rather than defaults.
     */
    @Transactional(readOnly = true)
    public StatementContext statementContext(String accountId) {
        Account account = require(accountId);
        return new StatementContext(
                account.getAccountId(),
                account.getCifNo(),
                account.getBranchCode(),
                account.getMaskedAccountNumber(),
                account.getAccountName(),
                account.getStatus().name(),
                account.getCurrency(),
                account.getLedgerBalance());
    }

    /**
     * Serves transaction-service's card-service contract from this service, avoiding a
     * separate card module. The caller checks that status is "ACTIVE", that
     * accountHolderId matches the account's holder, and that linkedAccountId matches the
     * source account.
     */
    @Transactional(readOnly = true)
    public CardContext cardContext(String cardId, String accountHolderId) {
        LinkedCard card = cards.findById(cardId)
                .orElseThrow(() -> ApiException.notFound("CARD_NOT_FOUND", "No card with id " + cardId));
        if (accountHolderId != null && !card.getCifNo().equals(accountHolderId)) {
            throw ApiException.forbidden("CARD_NOT_LINKED", "Card does not belong to this holder");
        }
        return new CardContext(
                card.getCardId(),
                card.getCifNo(),
                card.getAccountId(),
                card.getStatus(),
                card.getCurrency());
    }

    private Account require(String accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND",
                        "No account with id " + accountId));
    }
}
