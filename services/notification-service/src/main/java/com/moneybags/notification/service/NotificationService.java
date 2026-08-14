package com.moneybags.notification.service;

import com.moneybags.notification.api.ApiModels.*;
import com.moneybags.notification.client.CustomerPreferencesClient;
import com.moneybags.notification.entity.DeliveryAttempt;
import com.moneybags.notification.entity.Notification;
import com.moneybags.notification.entity.NotificationTemplate;
import com.moneybags.notification.repository.DeliveryAttemptRepository;
import com.moneybags.notification.repository.NotificationRepository;
import com.moneybags.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    static final String PENDING = "PENDING";
    static final String SENT = "SENT";
    static final String FAILED = "FAILED";
    static final String SUPPRESSED = "SUPPRESSED";

    private final NotificationRepository notifications;
    private final NotificationTemplateRepository templates;
    private final DeliveryAttemptRepository attempts;
    private final CustomerPreferencesClient preferencesClient;

    /**
     * Queues a notification. Idempotent on the producer's Idempotency-Key, because
     * producers deliver from an outbox with retries and a customer must not receive the
     * same message twice.
     */
    @Transactional
    public NotificationDetail queue(String dedupKey, String sourceService, SendRequest request) {
        var existing = notifications.findByDedupKey(dedupKey);
        if (existing.isPresent()) {
            return toDetail(existing.get());
        }

        String subject = request.subject();
        String body = request.body();
        if (request.templateCode() != null) {
            NotificationTemplate template = templates.findById(request.templateCode()).orElse(null);
            if (template == null) {
                log.warn("Unknown template {}; falling back to the literal body", request.templateCode());
            } else {
                subject = render(template.getSubjectTemplate(), request.variables());
                body = render(template.getBodyTemplate(), request.variables());
            }
        }
        if (body == null || body.isBlank()) {
            body = "(no content)";
        }

        // Opt-outs are honoured at queue time, so a suppressed message is visible in
        // history rather than silently never existing.
        String status = PENDING;
        if (request.cifNo() != null && !allowedByPreferences(request.cifNo(), request.channel())) {
            status = SUPPRESSED;
        }

        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .dedupKey(dedupKey)
                .channel(request.channel())
                .recipient(request.recipient())
                .templateCode(request.templateCode())
                .subject(subject)
                .body(body)
                .status(status)
                .attempts(0)
                .nextAttemptAt(Instant.now())
                .sourceService(sourceService)
                .correlationId(request.correlationId())
                .cifNo(request.cifNo())
                .createdAt(Instant.now())
                .build();
        return toDetail(notifications.save(notification));
    }

    private boolean allowedByPreferences(String cifNo, String channel) {
        try {
            CustomerPreferencesClient.Preferences preferences = preferencesClient.preferences(cifNo);
            return preferences == null || preferences.allows(channel);
        } catch (Exception ex) {
            // Fail open. A preferences lookup outage must not silently stop a customer
            // being told their account was frozen.
            log.warn("Could not read preferences for {}; sending anyway: {}", cifNo, ex.getMessage());
            return true;
        }
    }

    @Transactional
    public NotificationDetail retry(String notificationId) {
        Notification notification = notifications.findById(notificationId).orElseThrow();
        if (SENT.equals(notification.getStatus())) {
            return toDetail(notification);
        }
        notification.setStatus(PENDING);
        notification.setNextAttemptAt(Instant.now());
        notification.setLastError(null);
        return toDetail(notifications.save(notification));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDetail> search(String status, String cifNo, String recipient,
                                                   int page, int size) {
        Page<Notification> result = notifications.search(blankToNull(status), blankToNull(cifNo),
                blankToNull(recipient), PageRequest.of(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::toDetail).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public NotificationDetail detail(String notificationId) {
        return notifications.findById(notificationId).map(this::toDetail).orElse(null);
    }

    void recordAttempt(Notification notification, String outcome, String detail) {
        attempts.save(DeliveryAttempt.builder()
                .notificationId(notification.getNotificationId())
                .attemptNo(notification.getAttempts())
                .outcome(outcome)
                .detail(detail == null ? null
                        : detail.length() > 500 ? detail.substring(0, 500) : detail)
                .attemptedAt(Instant.now())
                .build());
    }

    /** Minimal {{key}} substitution; a missing variable renders as an empty string. */
    private String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        String rendered = template;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}",
                        entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
            }
        }
        return rendered.replaceAll("\\{\\{[^}]*}}", "");
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    NotificationDetail toDetail(Notification notification) {
        return new NotificationDetail(
                notification.getNotificationId(), notification.getChannel(), notification.getRecipient(),
                notification.getTemplateCode(), notification.getSubject(), notification.getBody(),
                notification.getStatus(), notification.getAttempts(), notification.getLastError(),
                notification.getCifNo(), notification.getCorrelationId(),
                notification.getCreatedAt(), notification.getSentAt());
    }
}
