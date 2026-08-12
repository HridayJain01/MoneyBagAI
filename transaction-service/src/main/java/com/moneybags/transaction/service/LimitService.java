package com.moneybags.transaction.service;

import com.moneybags.transaction.api.TransactionModels.LimitQuote;
import com.moneybags.transaction.domain.*;
import com.moneybags.transaction.entity.TransactionLimitRule;
import com.moneybags.transaction.exception.DomainException;
import com.moneybags.transaction.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.Comparator;

@Service @RequiredArgsConstructor
public class LimitService {
    private final TransactionLimitRuleRepository rules;
    private final TransactionRepository transactions;

    @Transactional(readOnly=true)
    public LimitQuote quote(String accountId,TransactionType type,PaymentRail rail,PaymentChannel channel,String currency,BigDecimal amount){
        TransactionLimitRule rule=rules.findActive(currency,Instant.now()).stream()
                .filter(r->r.getType()==null||r.getType()==type).filter(r->r.getRail()==null||r.getRail()==rail).filter(r->r.getChannel()==null||r.getChannel()==channel)
                .max(Comparator.comparingInt(TransactionLimitRule::getPriority)).orElse(null);
        if(rule==null) return new LimitQuote(type,rail,channel,currency,amount,null,null,null,null,true,false,null);
        String reason=null;
        if(rule.getMinAmount()!=null&&amount.compareTo(rule.getMinAmount())<0) reason="Amount is below the configured minimum";
        if(rule.getMaxAmount()!=null&&amount.compareTo(rule.getMaxAmount())>0) reason="Amount exceeds the configured per-transaction limit";
        if(reason==null&&rule.getDailyLimit()!=null){
            LocalDate today=LocalDate.now(ZoneOffset.UTC); BigDecimal used=transactions.sumDailyUsage(accountId,today.atStartOfDay().toInstant(ZoneOffset.UTC),today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
            if(used.add(amount).compareTo(rule.getDailyLimit())>0) reason="Amount exceeds the configured cumulative daily limit";
        }
        boolean approval=rule.getApprovalThreshold()!=null&&amount.compareTo(rule.getApprovalThreshold())>=0;
        return new LimitQuote(type,rail,channel,currency,amount,rule.getMinAmount(),rule.getMaxAmount(),rule.getDailyLimit(),rule.getApprovalThreshold(),reason==null,approval,reason);
    }
    public LimitQuote validate(String accountId,TransactionType type,PaymentRail rail,PaymentChannel channel,String currency,BigDecimal amount){
        LimitQuote quote=quote(accountId,type,rail,channel,currency,amount);
        if(!quote.allowed()) throw DomainException.invalid(type==TransactionType.RTGS&&quote.minAmount()!=null?"RTGS_MINIMUM_VIOLATION":"LIMIT_EXCEEDED",quote.reason());
        return quote;
    }
}
