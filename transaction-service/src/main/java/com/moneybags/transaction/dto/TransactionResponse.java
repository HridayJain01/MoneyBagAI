package com.moneybags.transaction.dto;
import com.moneybags.transaction.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record TransactionResponse(Long txnId, String txnRef, String requestRef, String accountNo,
        TransactionType txnType, DebitCredit drCr, BigDecimal amount, BigDecimal runningBalance,
        String counterpartyAcct, String narration, TransactionStatus status,
        LocalDateTime txnDate, String postedBy) {}
