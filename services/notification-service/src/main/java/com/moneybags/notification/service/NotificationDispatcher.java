package com.moneybags.notification.service;

import com.moneybags.notification.delivery.DeliveryChannel;
import com.moneybags.notification.entity.Notification;
import com.moneybags.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Drains PENDING notifications on a schedule with exponential backoff.
 *
 * <p>Never participates in a financial commit: producers queue a row and return, so a
 * provider outage delays alerts but cannot roll back a banking transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notifications;
    private final NotificationService notificationService;
    private final List<DeliveryChannel> channels;

    @Value("${moneybags.notification.batch-size:50}")
    private int batchSize;

    @Value("${moneybags.notification.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${moneybags.notification.fixed-delay-ms:5000}")
    @Transactional
    public void dispatch() {
        for (Notification notification : notifications.findDeliverable(
                NotificationService.PENDING, Instant.now(), PageRequest.of(0, batchSize))) {
            deliver(notification);
        }
    }

    private void deliver(Notification notification) {
        notification.setAttempts(notification.getAttempts() + 1);
        DeliveryChannel channel = channels.stream()
                .filter(candidate -> candidate.supports(notification.getChannel()))
                .findFirst()
                .orElse(null);

        if (channel == null) {
            fail(notification, "No delivery channel supports " + notification.getChannel());
            return;
        }

        try {
            channel.send(notification);
            notification.setStatus(NotificationService.SENT);
            notification.setSentAt(Instant.now());
            notification.setLastError(null);
            notificationService.recordAttempt(notification, "SUCCESS", null);
            notifications.save(notification);
        } catch (Exception ex) {
            fail(notification, ex.getMessage());
        }
    }

    private void fail(Notification notification, String reason) {
        notificationService.recordAttempt(notification, "FAILURE", reason);
        notification.setLastError(reason == null ? "unknown"
                : reason.length() > 500 ? reason.substring(0, 500) : reason);

        if (notification.getAttempts() >= maxAttempts) {
            notification.setStatus(NotificationService.FAILED);
            log.error("Notification {} failed permanently after {} attempts: {}",
                    notification.getNotificationId(), notification.getAttempts(), reason);
        } else {
            long backoffSeconds = Math.min(300L, 1L << Math.min(8, notification.getAttempts()));
            notification.setNextAttemptAt(Instant.now().plus(backoffSeconds, ChronoUnit.SECONDS));
        }
        notifications.save(notification);
    }
}
