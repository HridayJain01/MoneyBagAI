package com.moneybags.account.service;

import com.moneybags.account.api.ApiModels.*;
import com.moneybags.account.entity.*;
import com.moneybags.account.repository.*;
import com.moneybags.account.security.RequestActor;
import com.moneybags.account.support.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Account queries, lifecycle transitions, holders, manual holds and limits. */
@Service
@RequiredArgsConstructor
public class AccountServicingService {

    private final AccountRepository accounts;
    private final AccountHolderRepository holders;
    private final FundsHoldRepository holds;
    private final BalanceHistoryRepository balanceHistory;
    private final AccountStatusHistoryRepository statusHistory;
    private final AccountLimitsRepository limits;
    private final AccountEventPublisher eventPublisher;
    private final AccountProductOwnershipService productOwnerships;

    // --- Queries -----------------------------------------------------------

    @Transactional(readOnly = true)
    public AccountDetail detail(RequestActor actor, String accountId) {
        actor.require(RequestActor.PERMISSION_VIEW);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());
        return toDetail(account);
    }

    @Transactional(readOnly = true)
    public AccountDetail byNumber(RequestActor actor, String accountNumber) {
        actor.require(RequestActor.PERMISSION_VIEW);
        Account account = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND",
                        "No account with number " + accountNumber));
        actor.requireBranchAccess(account.getBranchCode());
        return toDetail(account);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountDetail> search(RequestActor actor, String cifNo, String requestedBranchCode,
                                              String productCode, AccountStatus status, int page, int size) {
        actor.require(RequestActor.PERMISSION_VIEW);
        String branchCode = blankToNull(requestedBranchCode);
        if (branchCode != null) {
            actor.requireBranchAccess(branchCode);
        } else if (!actor.canAccessAllBranches()) {
            branchCode = actor.branchCode();
        }
        Page<Account> result = accounts.search(blankToNull(cifNo), branchCode,
                blankToNull(productCode), status, PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::toDetail).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public BalanceView balance(RequestActor actor, String accountId) {
        actor.require(RequestActor.PERMISSION_VIEW);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());
        return new BalanceView(
                account.getAccountId(), account.getCurrency(), account.getLedgerBalance(),
                account.getHeldAmount(), account.availableBalance(), account.getMinBalance(),
                account.getOverdraftLimit(), Instant.now());
    }

    @Transactional(readOnly = true)
    public PageResponse<BalanceHistoryEntry> balanceHistory(RequestActor actor, String accountId,
                                                            int page, int size) {
        actor.require(RequestActor.PERMISSION_VIEW);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());
        Page<BalanceHistory> result = balanceHistory.findByAccountIdOrderByCreatedAtDesc(
                accountId, PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream()
                .map(entry -> new BalanceHistoryEntry(entry.getHistoryId(), entry.getTransactionId(),
                        entry.getTransactionReference(), entry.getDirection().name(), entry.getAmount(),
                        entry.getLedgerBalanceBefore(), entry.getLedgerBalanceAfter(),
                        entry.getBusinessDate(), entry.getCreatedAt()))
                .toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryEntry> statusHistory(RequestActor actor, String accountId) {
        actor.require(RequestActor.PERMISSION_VIEW);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());
        return statusHistory.findByAccountIdOrderByChangedAtDesc(accountId).stream()
                .map(entry -> new StatusHistoryEntry(entry.getId(), entry.getFromStatus(),
                        entry.getToStatus(), entry.getReason(), entry.getChangedByEmployeeId(),
                        entry.getSource(), entry.getChangedAt()))
                .toList();
    }

    // --- Lifecycle ---------------------------------------------------------

    /**
     * Applies a status transition with the state machine's legality rules.
     *
     * <p>Closure is refused while the account still holds money or has funds on hold --
     * closing over a live balance would strand it.
     */
    @Transactional
    public AccountDetail changeStatus(RequestActor actor, String accountId,
                                      AccountStatus target, String reason) {
        actor.require(RequestActor.PERMISSION_STATUS_MANAGE);
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "No account " + accountId));
        actor.requireBranchAccess(account.getBranchCode());

        AccountStatus current = account.getStatus();
        if (current == target) {
            return toDetail(account);
        }
        if (current.isTerminal()) {
            throw ApiException.conflict("ACCOUNT_CLOSED", "A closed account cannot change status");
        }
        if (target == AccountStatus.CLOSED) {
            if (account.getLedgerBalance().compareTo(BigDecimal.ZERO) != 0) {
                throw ApiException.unprocessable("BALANCE_NOT_SETTLED",
                        "Account still holds " + account.getLedgerBalance() + "; settle before closing");
            }
            if (account.getHeldAmount().compareTo(BigDecimal.ZERO) != 0) {
                throw ApiException.unprocessable("HOLDS_OUTSTANDING",
                        "Account has funds on hold; release them before closing");
            }
            account.setClosedOn(LocalDate.now(ZoneOffset.UTC));
        }
        if (target == AccountStatus.DORMANT) {
            account.setDormantSince(LocalDate.now(ZoneOffset.UTC));
        }
        if (target == AccountStatus.ACTIVE) {
            account.setDormantSince(null);
        }

        statusHistory.save(AccountStatusHistory.builder()
                .accountId(accountId)
                .fromStatus(current.name())
                .toStatus(target.name())
                .reason(reason)
                .changedByEmployeeId(actor.employeeId())
                .source("STAFF")
                .changedAt(Instant.now())
                .build());

        account.setStatus(target);
        accounts.saveAndFlush(account);
        productOwnerships.syncPrimaryStatus(account);
        eventPublisher.enqueueAccountEvent(account, "ACCOUNT_" + target.name());
        return toDetail(account);
    }

    // --- Holders -----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<HolderDetail> holders(RequestActor actor, String accountId) {
        actor.require(RequestActor.PERMISSION_VIEW);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());
        return holders.findByAccountIdOrderByHolderSequence(accountId).stream()
                .map(holder -> new HolderDetail(holder.getHolderId(), holder.getCifNo(),
                        holder.getHolderRole(), holder.getHolderSequence(), holder.getStatus(),
                        holder.getAddedAt()))
                .toList();
    }

    @Transactional
    public HolderDetail addHolder(RequestActor actor, String accountId, AddHolderRequest request) {
        actor.require(RequestActor.PERMISSION_STATUS_MANAGE);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());

        List<AccountHolder> existing = holders.findByAccountIdOrderByHolderSequence(accountId);
        if (existing.stream().anyMatch(holder -> holder.getCifNo().equals(request.cifNo()))) {
            throw ApiException.conflict("HOLDER_ALREADY_PRESENT",
                    request.cifNo() + " is already a holder on this account");
        }
        // Only one PRIMARY holder; additional holders join as JOINT.
        String role = request.holderRole() == null ? "JOINT" : request.holderRole();
        if ("PRIMARY".equals(role) && !existing.isEmpty()) {
            throw ApiException.conflict("PRIMARY_HOLDER_EXISTS",
                    "This account already has a primary holder");
        }

        AccountHolder holder = holders.save(AccountHolder.builder()
                .holderId(UUID.randomUUID().toString())
                .accountId(accountId)
                .cifNo(request.cifNo())
                .holderRole(role)
                .holderSequence(existing.size() + 1)
                .status("ACTIVE")
                .addedAt(Instant.now())
                .build());
        return new HolderDetail(holder.getHolderId(), holder.getCifNo(), holder.getHolderRole(),
                holder.getHolderSequence(), holder.getStatus(), holder.getAddedAt());
    }

    // --- Manual holds and liens --------------------------------------------

    @Transactional(readOnly = true)
    public List<HoldDetail> holds(RequestActor actor, String accountId) {
        actor.require(RequestActor.PERMISSION_VIEW);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());
        return holds.findByAccountIdAndStatus(accountId, HoldStatus.HELD).stream()
                .map(this::toHoldDetail)
                .toList();
    }

    /** Staff-placed lien or operational hold, distinct from a transaction hold. */
    @Transactional
    public HoldDetail placeManualHold(RequestActor actor, String accountId, ManualHoldRequest request) {
        actor.require(RequestActor.PERMISSION_STATUS_MANAGE);
        Account account = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "No account " + accountId));
        actor.requireBranchAccess(account.getBranchCode());

        if (account.availableBalance().compareTo(request.amount()) < 0) {
            throw ApiException.unprocessable("INSUFFICIENT_AVAILABLE_BALANCE",
                    "Available balance is less than the requested hold");
        }

        FundsHold hold = holds.save(FundsHold.builder()
                .holdId(UUID.randomUUID().toString())
                .accountId(accountId)
                .amount(request.amount())
                .currency(account.getCurrency())
                .reason(request.reason())
                .holdType(HoldType.valueOf(request.holdType() == null ? "MANUAL" : request.holdType()))
                .status(HoldStatus.HELD)
                .placedBy(actor.employeeId())
                .createdAt(Instant.now())
                .build());

        account.setHeldAmount(account.getHeldAmount().add(request.amount()));
        accounts.save(account);
        return toHoldDetail(hold);
    }

    // --- Limits ------------------------------------------------------------

    @Transactional(readOnly = true)
    public LimitsView limits(RequestActor actor, String accountId) {
        actor.require(RequestActor.PERMISSION_VIEW);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());
        return limits.findById(accountId)
                .map(value -> new LimitsView(accountId, value.getPerTransactionLimit(),
                        value.getDailyWithdrawalLimit(), value.getUpdatedAt()))
                // No override set: fall back to the product's daily withdrawal ceiling.
                .orElseGet(() -> new LimitsView(accountId, BigDecimal.ZERO, BigDecimal.ZERO, null));
    }

    @Transactional
    public LimitsView setLimits(RequestActor actor, String accountId, LimitsRequest request) {
        actor.require(RequestActor.PERMISSION_STATUS_MANAGE);
        Account account = require(accountId);
        actor.requireBranchAccess(account.getBranchCode());
        AccountLimits saved = limits.save(AccountLimits.builder()
                .accountId(accountId)
                .perTransactionLimit(request.perTransactionLimit())
                .dailyWithdrawalLimit(request.dailyWithdrawalLimit())
                .updatedAt(Instant.now())
                .build());
        return new LimitsView(accountId, saved.getPerTransactionLimit(),
                saved.getDailyWithdrawalLimit(), saved.getUpdatedAt());
    }

    // ------------------------------------------------------------------

    private Account require(String accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND",
                        "No account with id " + accountId));
    }

    private HoldDetail toHoldDetail(FundsHold hold) {
        return new HoldDetail(hold.getHoldId(), hold.getTransactionId(), hold.getAmount(),
                hold.getCurrency(), hold.getReason(), hold.getHoldType().name(),
                hold.getStatus().name(), hold.getCreatedAt(), hold.getReleasedAt());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private AccountDetail toDetail(Account account) {
        return new AccountDetail(
                account.getAccountId(), account.getAccountNumber(), account.getMaskedAccountNumber(),
                account.getAccountName(), account.getCifNo(), account.getProductCode(),
                account.getBranchCode(), account.getCurrency(), account.getStatus().name(),
                account.getLedgerBalance(), account.getHeldAmount(), account.availableBalance(),
                account.getMinBalance(), account.getOverdraftLimit(), account.getInterestRate(),
                account.getTenureMonths(), account.getMaturityDate(), account.getOpenedOn(),
                account.getClosedOn(), account.getDormantSince(), account.getVersion());
    }
}
