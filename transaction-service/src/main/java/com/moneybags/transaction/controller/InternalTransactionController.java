package com.moneybags.transaction.controller;

import com.moneybags.transaction.api.TransactionModels.CallbackRequest;
import com.moneybags.transaction.entity.Transaction;
import com.moneybags.transaction.service.CallbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController @RequestMapping("/internal/v1/transactions") @RequiredArgsConstructor @SecurityRequirements
public class InternalTransactionController {
    private final CallbackService callbacks;
    @PostMapping("/{id}/settle") public Transaction settle(@PathVariable String id,@Valid @RequestBody CallbackRequest body){return callbacks.settle(id,body);}
    @PostMapping("/{id}/fail") public Transaction fail(@PathVariable String id,@Valid @RequestBody CallbackRequest body){return callbacks.fail(id,body);}
    @PostMapping("/{id}/cheque-clearing") public Transaction cheque(@PathVariable String id,@Valid @RequestBody CallbackRequest body){return callbacks.cheque(id,body);}
}
