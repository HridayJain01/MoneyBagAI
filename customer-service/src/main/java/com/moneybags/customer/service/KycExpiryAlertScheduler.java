package com.moneybags.customer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class KycExpiryAlertScheduler {
    private final KycDocumentService kycDocumentService;

    @Scheduled(cron = "${customer.kyc-expiry-alert-cron:0 0 8 * * *}")
    public void createDailyExpiryAlerts() {
        kycDocumentService.processExpiryAlerts(LocalDate.now());
    }
}
