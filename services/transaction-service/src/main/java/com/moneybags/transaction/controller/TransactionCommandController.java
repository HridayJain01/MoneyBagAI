package com.moneybags.transaction.controller;

import com.moneybags.transaction.api.TransactionModels.*;
import com.moneybags.transaction.api.ProductPurchaseRequest;
import com.moneybags.transaction.api.ProductPurchaseResponse;
import com.moneybags.transaction.domain.*;
import com.moneybags.transaction.entity.Transaction;
import com.moneybags.transaction.security.*;
import com.moneybags.transaction.service.TransactionOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/transactions") @RequiredArgsConstructor
public class TransactionCommandController {
    private final TransactionOrchestrator service; private final RequestActorResolver actors;
    @PostMapping("/deposits") public ResponseEntity<Transaction> deposit(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.DEPOSIT,PaymentRail.CASH,body,key,actors.resolve(request)));}
    @PostMapping("/withdrawals") public ResponseEntity<Transaction> withdrawal(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.WITHDRAWAL,PaymentRail.CASH,body,key,actors.resolve(request)));}
    @PostMapping("/transfers/internal") public ResponseEntity<Transaction> internal(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.INTERNAL_TRANSFER,PaymentRail.INTERNAL,body,key,actors.resolve(request)));}
    @PostMapping("/transfers/neft") public ResponseEntity<Transaction> neft(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.NEFT,PaymentRail.NEFT,body,key,actors.resolve(request)));}
    @PostMapping("/transfers/rtgs") public ResponseEntity<Transaction> rtgs(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.RTGS,PaymentRail.RTGS,body,key,actors.resolve(request)));}
    @PostMapping("/transfers/imps") public ResponseEntity<Transaction> imps(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.IMPS,PaymentRail.IMPS,body,key,actors.resolve(request)));}
    @PostMapping("/transfers/upi") public ResponseEntity<Transaction> upi(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.UPI,PaymentRail.UPI,body,key,actors.resolve(request)));}
    @PostMapping("/cheques") public ResponseEntity<Transaction> cheque(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.CHEQUE,PaymentRail.CHEQUE,body,key,actors.resolve(request)));}
    @PostMapping("/card-payments") public ResponseEntity<Transaction> card(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateRequest body,HttpServletRequest request){return created(service.create(TransactionType.CARD_PAYMENT,PaymentRail.CARD,body,key,actors.resolve(request)));}
    @PostMapping("/product-purchases") public ResponseEntity<ProductPurchaseResponse> productPurchase(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ProductPurchaseRequest body,HttpServletRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.createProductPurchase(body,key,actors.resolve(request)));}
    @PostMapping("/{id}/approve") public Transaction approve(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,HttpServletRequest request){return service.approve(id,key,actors.resolve(request));}
    @PostMapping("/{id}/reject") public Transaction reject(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ActionRequest body,HttpServletRequest request){return service.reject(id,body.reason(),key,actors.resolve(request));}
    @PostMapping("/{id}/cancel") public Transaction cancel(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ActionRequest body,HttpServletRequest request){return service.cancel(id,body.reason(),key,actors.resolve(request));}
    @PostMapping("/{id}/reversals") public ResponseEntity<Transaction> reversal(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReversalRequest body,HttpServletRequest request){return created(service.reverse(id,body.reason(),key,actors.resolve(request)));}
    private ResponseEntity<Transaction> created(Transaction tx){return ResponseEntity.status(HttpStatus.CREATED).body(tx);}
}
