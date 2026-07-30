package com.moneybags.transaction.controller;
import com.moneybags.transaction.dto.*;
import com.moneybags.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
@RestController @RequestMapping("/api/v1/transactions") @RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;
    @PostMapping
    ResponseEntity<TransactionResponse> post(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.post(request));
    }
    @GetMapping("/{txnId}")
    TransactionResponse findById(@PathVariable Long txnId) { return service.findById(txnId); }
    @GetMapping
    List<TransactionResponse> findForAccount(
            @RequestParam String accountNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.findForAccount(accountNo, from, to);
    }
}
