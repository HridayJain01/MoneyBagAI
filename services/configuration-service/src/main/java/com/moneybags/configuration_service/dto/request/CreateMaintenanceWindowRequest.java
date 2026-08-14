package com.moneybags.configuration_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateMaintenanceWindowRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotNull(message = "startsAt is required")
    private LocalDateTime startsAt;

    @NotNull(message = "endsAt is required")
    private LocalDateTime endsAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
}
