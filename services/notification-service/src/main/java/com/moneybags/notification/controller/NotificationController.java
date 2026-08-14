package com.moneybags.notification.controller;

import com.moneybags.notification.api.ApiModels.*;
import com.moneybags.notification.entity.NotificationTemplate;
import com.moneybags.notification.repository.NotificationTemplateRepository;
import com.moneybags.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationTemplateRepository templates;

    /** Producer path. Idempotent on Idempotency-Key. */
    @Operation(summary = "Queue a notification")
    @PostMapping("/internal/v1/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationDetail queue(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Service-Name", required = false) String serviceName,
            @Valid @RequestBody SendRequest request) {
        String dedupKey = (idempotencyKey == null || idempotencyKey.isBlank())
                ? UUID.randomUUID().toString()
                : idempotencyKey;
        return notificationService.queue(dedupKey, serviceName == null ? "unknown" : serviceName, request);
    }

    @GetMapping("/api/v1/notifications")
    public PageResponse<NotificationDetail> search(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String cifNo,
                                                   @RequestParam(required = false) String recipient,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "25") int size) {
        return notificationService.search(status, cifNo, recipient, page, size);
    }

    @GetMapping("/api/v1/notifications/{notificationId}")
    public ResponseEntity<NotificationDetail> detail(@PathVariable String notificationId) {
        NotificationDetail detail = notificationService.detail(notificationId);
        return detail == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(detail);
    }

    @Operation(summary = "Requeue a failed notification")
    @PostMapping("/api/v1/notifications/{notificationId}/retry")
    public NotificationDetail retry(@PathVariable String notificationId) {
        return notificationService.retry(notificationId);
    }

    @GetMapping("/api/v1/notification-templates")
    public List<TemplateDetail> templates() {
        return templates.findAll().stream().map(this::toDetail).toList();
    }

    @PostMapping("/api/v1/notification-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateDetail createTemplate(@Valid @RequestBody TemplateRequest request) {
        NotificationTemplate template = NotificationTemplate.builder()
                .templateCode(request.templateCode())
                .channel(request.channel())
                .subjectTemplate(request.subjectTemplate())
                .bodyTemplate(request.bodyTemplate())
                .locale(request.locale() == null ? "en-IN" : request.locale())
                .active(true)
                .updatedAt(Instant.now())
                .build();
        return toDetail(templates.save(template));
    }

    private TemplateDetail toDetail(NotificationTemplate template) {
        return new TemplateDetail(template.getTemplateCode(), template.getChannel(),
                template.getSubjectTemplate(), template.getBodyTemplate(), template.getLocale(),
                Boolean.TRUE.equals(template.getActive()), template.getUpdatedAt());
    }
}
