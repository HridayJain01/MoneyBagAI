package com.moneybags.account.controller;
import com.moneybags.account.dto.*;
import com.moneybags.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/accounts") @RequiredArgsConstructor
public class AccountController {
    private final AccountService service;
    @PostMapping
    ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
    @GetMapping("/{accountNo}")
    AccountResponse findByNumber(@PathVariable String accountNo) { return service.findByNumber(accountNo); }
    @GetMapping
    List<AccountResponse> findByCif(@RequestParam String cifNo) { return service.findByCif(cifNo); }
}
