package com.moneybags.notification.delivery;

import com.moneybags.notification.entity.Notification;

/**
 * Seam for a real provider adapter.
 *
 * <p>Only a logging implementation ships today -- there is no SMTP or SMS gateway in this
 * deployment. Adding a real one means implementing this interface and marking it
 * {@code @Primary}; nothing else in the service changes.
 */
public interface DeliveryChannel {

    boolean supports(String channel);

    /**
     * @throws Exception to signal a retryable failure; the dispatcher records the attempt
     *                   and backs off
     */
    void send(Notification notification) throws Exception;
}
