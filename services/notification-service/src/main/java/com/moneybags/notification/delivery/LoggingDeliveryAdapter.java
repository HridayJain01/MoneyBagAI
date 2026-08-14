package com.moneybags.notification.delivery;

import com.moneybags.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes the rendered message to the service log and treats that as delivery.
 *
 * <p>Recipients are masked: an operational log is not the place for a customer's full
 * email address or mobile number.
 */
@Slf4j
@Component
public class LoggingDeliveryAdapter implements DeliveryChannel {

    @Override
    public boolean supports(String channel) {
        return true;
    }

    @Override
    public void send(Notification notification) {
        log.info("NOTIFY [{}] to {} | subject={} | body={}",
                notification.getChannel(),
                mask(notification.getRecipient()),
                notification.getSubject() == null ? "-" : notification.getSubject(),
                notification.getBody());
    }

    private String mask(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            return "unknown";
        }
        int at = recipient.indexOf('@');
        if (at > 1) {
            return recipient.charAt(0) + "***" + recipient.substring(at);
        }
        if (recipient.length() <= 4) {
            return "***";
        }
        return "***" + recipient.substring(recipient.length() - 4);
    }
}
