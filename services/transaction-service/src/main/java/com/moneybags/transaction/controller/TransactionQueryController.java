package com.moneybags.transaction.controller;

import com.moneybags.transaction.api.TransactionModels.*;
import com.moneybags.transaction.api.ProductPurchaseResponse;
import com.moneybags.transaction.config.TransactionProperties;
import com.moneybags.transaction.domain.*;
import com.moneybags.transaction.security.*;
import com.moneybags.transaction.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController @RequiredArgsConstructor
public class TransactionQueryController {
    private final TransactionQueryService queries; private final LimitService limits; private final RequestActorResolver actors; private final TransactionProperties properties;
    @GetMapping("/api/v1/transactions") public Page<TransactionView> search(@RequestParam(required=false)String account,@RequestParam(required=false)String reference,
            @RequestParam(required=false)TransactionStatus status,@RequestParam(required=false)PaymentRail rail,@RequestParam(required=false)TransactionType transactionType,
            @RequestParam(required=false)BigDecimal minAmount,@RequestParam(required=false)BigDecimal maxAmount,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant to,
            @RequestParam(required=false)String createdBy,@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC)Pageable page,HttpServletRequest request){return queries.search(account,reference,status,rail,transactionType,minAmount,maxAmount,from,to,createdBy,page,actors.resolve(request));}
    @GetMapping("/api/v1/transactions/by-reference/{reference}") public TransactionView reference(@PathVariable String reference,HttpServletRequest request){return queries.byReference(reference,actors.resolve(request));}
    @GetMapping("/api/v1/transactions/{id}/status") public StatusView status(@PathVariable String id,HttpServletRequest request){return queries.status(id,actors.resolve(request));}
    @GetMapping("/api/v1/transactions/{id}") public TransactionView get(@PathVariable String id,HttpServletRequest request){return queries.byId(id,actors.resolve(request));}
    @GetMapping("/api/v1/transactions/{id}/product-purchase") public ProductPurchaseResponse productPurchase(@PathVariable String id,HttpServletRequest request){return queries.productPurchase(id,actors.resolve(request));}
    @GetMapping("/api/v1/accounts/{accountId}/transactions") public Page<TransactionView> account(@PathVariable String accountId,@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC)Pageable page,HttpServletRequest request){return queries.account(accountId,page,actors.resolve(request));}
    @GetMapping("/api/v1/accounts/{accountId}/mini-statement") public List<TransactionView> mini(@PathVariable String accountId,@RequestParam(required=false)Integer size,HttpServletRequest request){return queries.miniStatement(accountId,size==null?properties.getMiniStatementSize():size,actors.resolve(request));}
    @GetMapping("/api/v1/transactions/approvals") public Page<TransactionView> approvals(@RequestParam(required=false)String branch,@RequestParam(required=false)BigDecimal minAmount,@RequestParam(required=false)BigDecimal maxAmount,@RequestParam(required=false)PaymentRail rail,@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.ASC)Pageable page,HttpServletRequest request){return queries.approvals(branch,minAmount,maxAmount,rail,page,actors.resolve(request));}
    @GetMapping("/api/v1/transactions/limits/quote") public LimitQuote quote(@RequestParam String accountId,@RequestParam TransactionType transactionType,@RequestParam PaymentRail rail,@RequestParam PaymentChannel channel,@RequestParam(defaultValue="INR")String currency,@RequestParam BigDecimal amount,HttpServletRequest request){RequestActor actor=actors.resolve(request);actor.require("TRANSACTION_VIEW");return limits.quote(accountId,transactionType,rail,channel,currency,amount);}
}
