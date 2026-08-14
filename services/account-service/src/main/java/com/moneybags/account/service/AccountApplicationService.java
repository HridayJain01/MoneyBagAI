package com.moneybags.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.account.api.ApiModels.*;
import com.moneybags.account.client.CustomerClient;
import com.moneybags.account.client.ProductClient;
import com.moneybags.account.entity.*;
import com.moneybags.account.repository.*;
import com.moneybags.account.security.RequestActor;
import com.moneybags.account.support.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Account opening: application, maker-checker approval, and account creation.
 *
 * <p>Every mutation here is performed by an employee on behalf of a customer -- customers
 * are data in this system, never callers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private final AccountApplicationRepository applications;
    private final AccountApprovalRepository approvals;
    private final AccountRepository accounts;
    private final AccountHolderRepository holders;
    private final AccountStatusHistoryRepository statusHistory;
    private final ProductClient productClient;
    private final CustomerClient customerClient;
    private final AccountEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public ApplicationDetail create(RequestActor actor, CreateApplicationRequest request) {
        actor.require(RequestActor.PERMISSION_OPEN);

        // Both checks are remote and both must pass before an application exists at all;
        // creating a DRAFT against an ineligible CIF just produces work that cannot complete.
        requireEligibleCustomer(request.cifNo());
        ProductClient.EffectiveProduct product = resolveProduct(request.productCode());

        BigDecimal initialDeposit = request.initialDeposit() == null
                ? BigDecimal.ZERO : request.initialDeposit();
        if (product.requiresFunding()
                && initialDeposit.compareTo(product.minOpeningDeposit()) < 0) {
            throw ApiException.unprocessable("INSUFFICIENT_OPENING_DEPOSIT",
                    product.productCode() + " requires an opening deposit of at least "
                            + product.minOpeningDeposit());
        }

        AccountApplication application = AccountApplication.builder()
                .applicationId(UUID.randomUUID().toString())
                .applicationReference("APP-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 16).toUpperCase())
                .cifNo(request.cifNo())
                .productCode(product.productCode())
                .branchCode(actor.branchCode())
                .currency(product.currency())
                .accountName(request.accountName() == null
                        ? product.productName() + " - " + request.cifNo()
                        : request.accountName())
                .requestedInitialDeposit(initialDeposit)
                .status(ApplicationStatus.PENDING_APPROVAL)
                .makerEmployeeId(actor.employeeId())
                .productSnapshot(serialise(product))
                .correlationId(actor.correlationId())
                .build();

        return toDetail(applications.save(application));
    }

    /**
     * Approves an application and creates the account.
     *
     * <p>Maker and checker must differ -- that is the entire point of the control, and it
     * matches the rule transaction-service already enforces on transaction approval.
     */
    @Transactional
    public ApplicationDetail approve(RequestActor actor, String applicationId, DecisionRequest request) {
        actor.require(RequestActor.PERMISSION_APPROVE);
        AccountApplication application = require(applicationId);
        actor.requireBranchAccess(application.getBranchCode());

        if (application.getStatus().isDecided()) {
            throw ApiException.conflict("APPLICATION_ALREADY_DECIDED",
                    "Application is already " + application.getStatus());
        }
        if (application.getMakerEmployeeId().equals(actor.employeeId())) {
            throw ApiException.forbidden("MAKER_CANNOT_APPROVE",
                    "The employee who created an application cannot approve it");
        }

        Account account = createAccountFrom(application, actor);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setCheckerEmployeeId(actor.employeeId());
        // UNIQUE in the schema, so a concurrent double-approval cannot create two accounts.
        application.setCreatedAccountId(account.getAccountId());
        applications.save(application);

        approvals.save(AccountApproval.builder()
                .approvalId(UUID.randomUUID().toString())
                .applicationId(applicationId)
                .accountId(account.getAccountId())
                .empId(actor.employeeId())
                .decision("APPROVED")
                .remarks(request == null ? null : request.remarks())
                .decidedAt(Instant.now())
                .build());

        return toDetail(application);
    }

    @Transactional
    public ApplicationDetail reject(RequestActor actor, String applicationId, RejectionRequest request) {
        actor.require(RequestActor.PERMISSION_APPROVE);
        AccountApplication application = require(applicationId);
        actor.requireBranchAccess(application.getBranchCode());

        if (application.getStatus().isDecided()) {
            throw ApiException.conflict("APPLICATION_ALREADY_DECIDED",
                    "Application is already " + application.getStatus());
        }
        if (application.getMakerEmployeeId().equals(actor.employeeId())) {
            throw ApiException.forbidden("MAKER_CANNOT_APPROVE",
                    "The employee who created an application cannot decide it");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setCheckerEmployeeId(actor.employeeId());
        application.setRejectionReason(request.reason());
        applications.save(application);

        approvals.save(AccountApproval.builder()
                .approvalId(UUID.randomUUID().toString())
                .applicationId(applicationId)
                .empId(actor.employeeId())
                .decision("REJECTED")
                .remarks(request.reason())
                .decidedAt(Instant.now())
                .build());

        return toDetail(application);
    }

    @Transactional
    public ApplicationDetail cancel(RequestActor actor, String applicationId) {
        AccountApplication application = require(applicationId);
        if (!application.getMakerEmployeeId().equals(actor.employeeId())) {
            throw ApiException.forbidden("NOT_APPLICATION_MAKER",
                    "Only the maker may cancel an application");
        }
        if (application.getStatus().isDecided()) {
            throw ApiException.conflict("APPLICATION_ALREADY_DECIDED",
                    "Application is already " + application.getStatus());
        }
        application.setStatus(ApplicationStatus.CANCELLED);
        return toDetail(applications.save(application));
    }

    @Transactional(readOnly = true)
    public ApplicationDetail detail(RequestActor actor, String applicationId) {
        AccountApplication application = require(applicationId);
        actor.requireBranchAccess(application.getBranchCode());
        return toDetail(application);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationDetail> search(RequestActor actor, String cifNo,
                                                  ApplicationStatus status, int page, int size) {
        // Staff without the cross-branch permission see only their own branch.
        String branchCode = actor.canAccessAllBranches() ? null : actor.branchCode();
        Page<AccountApplication> result = applications.search(
                blankToNull(cifNo), branchCode, status, PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::toDetail).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    // ------------------------------------------------------------------

    private Account createAccountFrom(AccountApplication application, RequestActor actor) {
        ProductClient.EffectiveProduct product = deserialise(application.getProductSnapshot());
        String accountNumber = generateAccountNumber(application.getBranchCode());

        // Opened directly ACTIVE unless the product mandates funding. Opening into
        // PENDING_ACTIVATION by default would block the first deposit, since
        // transaction-service refuses to act on anything whose status is not "ACTIVE".
        AccountStatus initialStatus = product.requiresFunding()
                ? AccountStatus.PENDING_ACTIVATION
                : AccountStatus.ACTIVE;

        Account account = Account.builder()
                .accountId(UUID.randomUUID().toString())
                .accountNumber(accountNumber)
                .maskedAccountNumber(mask(accountNumber))
                .accountName(application.getAccountName())
                .cifNo(application.getCifNo())
                .productCode(application.getProductCode())
                .branchCode(application.getBranchCode())
                .currency(application.getCurrency())
                .status(initialStatus)
                .ledgerBalance(BigDecimal.ZERO)
                .heldAmount(BigDecimal.ZERO)
                // Terms snapshotted, not referenced.
                .minBalance(nvl(product.minBalance()))
                .overdraftLimit(nvl(product.overdraftLimit()))
                .interestRate(nvl(product.interestRate()))
                .tenureMonths(product.tenureMonths())
                .maturityDate(product.tenureMonths() == null
                        ? null
                        : LocalDate.now(ZoneOffset.UTC).plusMonths(product.tenureMonths()))
                .openedOn(LocalDate.now(ZoneOffset.UTC))
                .applicationId(application.getApplicationId())
                .build();
        accounts.saveAndFlush(account);

        holders.save(AccountHolder.builder()
                .holderId(UUID.randomUUID().toString())
                .accountId(account.getAccountId())
                .cifNo(application.getCifNo())
                .holderRole("PRIMARY")
                .holderSequence(1)
                .status("ACTIVE")
                .addedAt(Instant.now())
                .build());

        statusHistory.save(AccountStatusHistory.builder()
                .accountId(account.getAccountId())
                .fromStatus(null)
                .toStatus(initialStatus.name())
                .reason("Account opened from application " + application.getApplicationReference())
                .changedByEmployeeId(actor.employeeId())
                .source("APPLICATION_APPROVAL")
                .changedAt(Instant.now())
                .build());

        eventPublisher.enqueueAccountEvent(account, "ACCOUNT_OPENED");
        return account;
    }

    /** Branch-prefixed so an account number carries its originating branch. */
    private String generateAccountNumber(String branchCode) {
        String digits = branchCode.replaceAll("\\D", "");
        String prefix = digits.isEmpty() ? "10" : String.format("%02d", Integer.parseInt(digits) % 100);
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = prefix + String.format("%012d",
                    ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
            if (accounts.findByAccountNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw ApiException.conflict("ACCOUNT_NUMBER_EXHAUSTED",
                "Could not allocate a unique account number");
    }

    private String mask(String accountNumber) {
        if (accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "X".repeat(accountNumber.length() - 4)
                + accountNumber.substring(accountNumber.length() - 4);
    }

    private void requireEligibleCustomer(String cifNo) {
        try {
            CustomerClient.EligibilityResponse eligibility = customerClient.eligibility(cifNo);
            if (eligibility != null && !eligibility.eligible()) {
                throw ApiException.unprocessable("CUSTOMER_NOT_ELIGIBLE",
                        eligibility.reason() == null
                                ? "Customer " + cifNo + " is not eligible to open an account"
                                : eligibility.reason());
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.unprocessable("CUSTOMER_NOT_VERIFIABLE",
                    "Could not verify customer " + cifNo + ": " + ex.getMessage());
        }
    }

    private ProductClient.EffectiveProduct resolveProduct(String productCode) {
        try {
            return productClient.effective(productCode);
        } catch (Exception ex) {
            throw ApiException.unprocessable("PRODUCT_NOT_AVAILABLE",
                    "Could not resolve product " + productCode + ": " + ex.getMessage());
        }
    }

    private String serialise(ProductClient.EffectiveProduct product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialise product snapshot", ex);
        }
    }

    private ProductClient.EffectiveProduct deserialise(String snapshot) {
        try {
            return objectMapper.readValue(snapshot, ProductClient.EffectiveProduct.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read product snapshot", ex);
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private AccountApplication require(String applicationId) {
        return applications.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND",
                        "No application with id " + applicationId));
    }

    private ApplicationDetail toDetail(AccountApplication application) {
        return new ApplicationDetail(
                application.getApplicationId(), application.getApplicationReference(),
                application.getCifNo(), application.getProductCode(), application.getBranchCode(),
                application.getCurrency(), application.getAccountName(),
                application.getRequestedInitialDeposit(), application.getStatus().name(),
                application.getMakerEmployeeId(), application.getCheckerEmployeeId(),
                application.getRejectionReason(), application.getCreatedAccountId(),
                application.getCreatedAt(), application.getUpdatedAt());
    }
}
