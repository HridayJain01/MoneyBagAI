package com.moneybags.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Communication preferences live in customer-service, which owns them. This service
 * reads them rather than keeping a second copy that would drift.
 */
@FeignClient(name = "customer-service")
public interface CustomerPreferencesClient {

    @GetMapping("/api/v1/customers/{cif}/communication-preferences")
    Preferences preferences(@PathVariable("cif") String cif);

    record Preferences(
            Boolean emailNotificationsEnabled,
            Boolean smsNotificationsEnabled,
            Boolean pushNotificationsEnabled,
            String preferredCommunicationChannel) {

        public boolean allows(String channel) {
            return switch (channel) {
                case "EMAIL" -> !Boolean.FALSE.equals(emailNotificationsEnabled);
                case "SMS" -> !Boolean.FALSE.equals(smsNotificationsEnabled);
                case "PUSH" -> !Boolean.FALSE.equals(pushNotificationsEnabled);
                default -> true;
            };
        }
    }
}
