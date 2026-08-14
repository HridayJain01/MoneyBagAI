package com.moneybags.configuration_service.dto.request;

import java.time.LocalDateTime;

public class UpdateMaintenanceWindowRequest {

    private String title;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
}
