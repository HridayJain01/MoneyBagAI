package com.moneybags.statement.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@FeignClient(name = "transaction-service")
public interface TransactionClient {
    @GetMapping("/api/v1/transactions")
    List<TransactionSummary> findTransactions(
            @RequestParam String accountNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to);
    record TransactionSummary(Long txnId, String txnRef, String txnType, String drCr,
                              BigDecimal amount, BigDecimal runningBalance, String narration,
                              String status, LocalDateTime txnDate) {}
}
