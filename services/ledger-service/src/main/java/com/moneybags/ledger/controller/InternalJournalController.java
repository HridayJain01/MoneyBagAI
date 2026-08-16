package com.moneybags.ledger.controller;

import com.moneybags.ledger.dto.JournalPostRequest;
import com.moneybags.ledger.dto.JournalResponse;
import com.moneybags.ledger.exception.LedgerException;
import com.moneybags.ledger.service.JournalPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/ledger")
@RequiredArgsConstructor
public class InternalJournalController {
    private final JournalPostingService postingService;

    @PostMapping("/journals")
    public JournalResponse post(@RequestHeader("X-Service-Name") String serviceName,
                                @Valid @RequestBody JournalPostRequest request) {
        if (!"transaction-service".equals(serviceName)) {
            throw new LedgerException(HttpStatus.FORBIDDEN, "SERVICE_AUTH_DENIED",
                    "Only transaction-service may post internal journals");
        }
        return postingService.post(request);
    }
}
