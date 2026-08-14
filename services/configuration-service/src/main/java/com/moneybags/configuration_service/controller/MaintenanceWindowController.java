package com.moneybags.configuration_service.controller;

import com.moneybags.configuration_service.dto.request.CreateMaintenanceWindowRequest;
import com.moneybags.configuration_service.dto.request.UpdateMaintenanceWindowRequest;
import com.moneybags.configuration_service.dto.request.UpdateStatusRequest;
import com.moneybags.configuration_service.entity.MaintenanceWindow;
import com.moneybags.configuration_service.service.MaintenanceWindowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Maintenance Windows", description = "Scheduled maintenance and status transitions")
@RestController
@RequestMapping("/api/v1/configuration/maintenance-windows")
public class MaintenanceWindowController {

    private final MaintenanceWindowService maintenanceWindowService;

    public MaintenanceWindowController(MaintenanceWindowService maintenanceWindowService) {
        this.maintenanceWindowService = maintenanceWindowService;
    }

    @GetMapping
    public List<MaintenanceWindow> getAll() {
        return maintenanceWindowService.getAll();
    }

    @GetMapping("/{id}")
    public MaintenanceWindow getById(@PathVariable Long id) {
        return maintenanceWindowService.getById(id);
    }

    @PostMapping
    public ResponseEntity<MaintenanceWindow> create(
            @Valid @RequestBody CreateMaintenanceWindowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceWindowService.create(request));
    }

    @PatchMapping("/{id}")
    public MaintenanceWindow update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateMaintenanceWindowRequest request) {
        return maintenanceWindowService.update(id, request);
    }

    @PostMapping("/{id}/status")
    public MaintenanceWindow transitionStatus(@PathVariable Long id,
                                              @Valid @RequestBody UpdateStatusRequest request) {
        return maintenanceWindowService.transitionStatus(id, request.getStatus());
    }
}
