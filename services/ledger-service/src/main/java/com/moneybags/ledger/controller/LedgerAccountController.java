package com.moneybags.ledger.controller;

import com.moneybags.ledger.dto.LedgerAccountResponse;
import com.moneybags.ledger.dto.LedgerBalanceResponse;
import com.moneybags.ledger.service.LedgerAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger/accounts")
@RequiredArgsConstructor
public class LedgerAccountController {
    private final LedgerAccountService service;

    @GetMapping
    @Operation(summary = "List GL accounts", description = "Returns accounting accounts and their current normal-side balances.")
    public List<LedgerAccountResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a GL account")
    @ApiResponse(responseCode = "404", description = "Ledger code does not exist")
    public LedgerAccountResponse findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @GetMapping("/{code}/balance")
    @Operation(summary = "Get a GL account balance")
    public LedgerBalanceResponse balance(@PathVariable String code) {
        return service.balance(code);
    }
}
