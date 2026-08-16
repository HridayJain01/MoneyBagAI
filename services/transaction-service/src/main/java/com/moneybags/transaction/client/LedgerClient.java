package com.moneybags.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@FeignClient(name = "ledger-service")
public interface LedgerClient {
    @PostMapping("/internal/v1/ledger/journals")
    JournalResponse post(@RequestHeader("X-Service-Name") String serviceName,
                         @RequestBody JournalPostRequest request);

    record JournalPostRequest(String journalReference, String transactionId, String journalType,
                              String description, String currencyCode, String createdBy,
                              List<JournalLineRequest> lines) {}

    record JournalLineRequest(String ledgerCode, String customerAccountId, String side,
                              BigDecimal amount, String description) {}

    record JournalResponse(Long id, String journalReference, String transactionId, String journalType,
                           String description, String status, String currencyCode,
                           BigDecimal totalDebit, BigDecimal totalCredit, Long reversalOfJournalId,
                           Instant createdAt, Instant postedAt, String createdBy,
                           List<JournalLineResponse> lines) {}

    record JournalLineResponse(Long id, int lineNumber, String ledgerCode, String ledgerAccountName,
                               String customerAccountId, String side, BigDecimal amount,
                               String description, Instant createdAt) {}
}
