package com.moneybags.account.controller;

import com.moneybags.account.api.InternalModels.*;
import com.moneybags.account.service.AccountQueryService;
import com.moneybags.account.service.HoldService;
import com.moneybags.account.service.ProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Service-trust surface, blocked at the gateway.
 *
 * <p>These endpoints deliberately require NO actor headers. transaction-service has no
 * Feign RequestInterceptor, so the only header it sends is Idempotency-Key; requiring
 * X-Employee-Id here would break every transaction in the system.
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirements
public class InternalAccountController {

    private final AccountQueryService queryService;
    private final HoldService holdService;
    private final ProjectionService projectionService;

    @Operation(summary = "Account state for transaction pre-flight checks")
    @GetMapping("/internal/v1/accounts/{accountId}/transaction-context")
    public AccountContext transactionContext(@PathVariable String accountId) {
        return queryService.transactionContext(accountId);
    }

    /** Insufficient funds must be a 409; the caller maps that status to INSUFFICIENT_FUNDS. */
    @Operation(summary = "Reserve funds for an in-flight transaction")
    @PostMapping("/internal/v1/accounts/{accountId}/holds")
    public HoldResponse reserve(@PathVariable String accountId,
                                @RequestHeader("Idempotency-Key") String idempotencyKey,
                                @Valid @RequestBody HoldRequest request) {
        return holdService.reserve(accountId, request);
    }

    /**
     * Normally a no-op: the debit projection already consumed the hold. Kept idempotent
     * so losing this call is harmless.
     */
    @Operation(summary = "Mark a hold consumed")
    @PostMapping("/internal/v1/accounts/{accountId}/holds/{holdId}/consume")
    public void consume(@PathVariable String accountId,
                        @PathVariable String holdId,
                        @RequestHeader("Idempotency-Key") String idempotencyKey) {
        holdService.consume(accountId, holdId);
    }

    /**
     * Must never return 4xx -- the caller treats any exception here as a failed
     * compensation. Idempotent on hold state, because the release key varies by reason.
     */
    @Operation(summary = "Release a hold")
    @PostMapping("/internal/v1/accounts/{accountId}/holds/{holdId}/release")
    public void release(@PathVariable String accountId,
                        @PathVariable String holdId,
                        @RequestHeader("Idempotency-Key") String idempotencyKey) {
        holdService.release(accountId, holdId, idempotencyKey);
    }

    /**
     * Applies a balance movement. Idempotent on both eventId and the Idempotency-Key,
     * because the caller retries up to ten times with exponential backoff.
     */
    @Operation(summary = "Apply an idempotent balance projection")
    @PostMapping("/internal/v1/account-projections")
    public void project(@RequestHeader("Idempotency-Key") String idempotencyKey,
                        @Valid @RequestBody ProjectionInstruction instruction) {
        projectionService.apply(idempotencyKey, instruction);
    }

    @Operation(summary = "Account metadata for statement generation")
    @GetMapping("/internal/v1/accounts/{accountId}/statement-context")
    public StatementContext statementContext(@PathVariable String accountId) {
        return queryService.statementContext(accountId);
    }

    /** Serves transaction-service's card-service contract. */
    @Operation(summary = "Card payment context")
    @GetMapping("/internal/v1/cards/{cardId}/payment-context")
    public CardContext cardContext(@PathVariable String cardId,
                                   @RequestParam(required = false) String accountHolderId) {
        return queryService.cardContext(cardId, accountHolderId);
    }
}
