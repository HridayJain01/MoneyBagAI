package com.moneybags.configuration_service.service;

import com.moneybags.configuration_service.dto.request.CreateMaintenanceWindowRequest;
import com.moneybags.configuration_service.dto.request.UpdateMaintenanceWindowRequest;
import com.moneybags.configuration_service.entity.MaintenanceWindow;
import com.moneybags.configuration_service.exception.ConflictException;
import com.moneybags.configuration_service.exception.NotFoundException;
import com.moneybags.configuration_service.repository.MaintenanceWindowRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaintenanceWindowService {

    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final AuditService auditService;

    public MaintenanceWindowService(MaintenanceWindowRepository maintenanceWindowRepository, AuditService auditService) {
        this.maintenanceWindowRepository = maintenanceWindowRepository;
        this.auditService = auditService;
    }

    public List<MaintenanceWindow> getAll() {
        return maintenanceWindowRepository.findAll();
    }

    public MaintenanceWindow getById(Long id) {
        return requireWindow(id);
    }

    public MaintenanceWindow create(CreateMaintenanceWindowRequest request) {
        MaintenanceWindow window = new MaintenanceWindow();
        window.setTitle(request.getTitle());
        window.setStartsAt(request.getStartsAt());
        window.setEndsAt(request.getEndsAt());
        window.setStatus("PLANNED");
        MaintenanceWindow saved = maintenanceWindowRepository.save(window);
        auditService.logChange("MAINTENANCE_WINDOW", String.valueOf(saved.getId()), null, describe(saved));
        return saved;
    }

    public MaintenanceWindow update(Long id, UpdateMaintenanceWindowRequest request) {
        MaintenanceWindow window = requireWindow(id);
        String oldValue = describe(window);
        if (request.getTitle() != null) window.setTitle(request.getTitle());
        if (request.getStartsAt() != null) window.setStartsAt(request.getStartsAt());
        if (request.getEndsAt() != null) window.setEndsAt(request.getEndsAt());
        MaintenanceWindow saved = maintenanceWindowRepository.save(window);
        auditService.logChange("MAINTENANCE_WINDOW", String.valueOf(id), oldValue, describe(saved));
        return saved;
    }

    public MaintenanceWindow transitionStatus(Long id, String newStatus) {
        MaintenanceWindow window = requireWindow(id);
        String oldValue = describe(window);
        if (!isAllowedTransition(window.getStatus(), newStatus)) {
            throw new ConflictException("Cannot transition maintenance window from "
                    + window.getStatus() + " to " + newStatus);
        }
        window.setStatus(newStatus);
        MaintenanceWindow saved = maintenanceWindowRepository.save(window);
        auditService.logChange("MAINTENANCE_WINDOW", String.valueOf(id), oldValue, describe(saved));
        return saved;
    }

    private MaintenanceWindow requireWindow(Long id) {
        return maintenanceWindowRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Maintenance window not found: " + id));
    }

    private boolean isAllowedTransition(String currentStatus, String newStatus) {
        return ("PLANNED".equals(currentStatus)
                && ("ACTIVE".equals(newStatus) || "CANCELLED".equals(newStatus)))
                || ("ACTIVE".equals(currentStatus)
                && ("COMPLETED".equals(newStatus) || "CANCELLED".equals(newStatus)));
    }

    private String describe(MaintenanceWindow window) {
        return "title=" + window.getTitle() + ", status=" + window.getStatus()
                + ", startsAt=" + window.getStartsAt() + ", endsAt=" + window.getEndsAt();
    }
}
