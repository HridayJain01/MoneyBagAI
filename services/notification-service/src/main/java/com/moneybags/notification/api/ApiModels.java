package com.moneybags.notification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {
    }

    /**
     * Producer contract. Either a templateCode plus variables, or a literal body.
     * cifNo is optional but enables the opt-out check against customer-service.
     */
    public record SendRequest(
            @NotBlank @Pattern(regexp = "EMAIL|SMS|PUSH") String channel,
            @NotBlank String recipient,
            String templateCode,
            String subject,
            String body,
            String cifNo,
            String correlationId,
            Map<String, Object> variables) {
    }

    public record NotificationDetail(
            String notificationId,
            String channel,
            String recipient,
            String templateCode,
            String subject,
            String body,
            String status,
            int attempts,
            String lastError,
            String cifNo,
            String correlationId,
            Instant createdAt,
            Instant sentAt) {
    }

    public record TemplateDetail(
            String templateCode,
            String channel,
            String subjectTemplate,
            String bodyTemplate,
            String locale,
            boolean active,
            Instant updatedAt) {
    }

    public record TemplateRequest(
            @NotBlank String templateCode,
            @NotBlank @Pattern(regexp = "EMAIL|SMS|PUSH") String channel,
            String subjectTemplate,
            @NotBlank String bodyTemplate,
            String locale) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }
}
