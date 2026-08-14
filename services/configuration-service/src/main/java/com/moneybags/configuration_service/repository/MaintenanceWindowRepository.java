package com.moneybags.configuration_service.repository;

import com.moneybags.configuration_service.entity.MaintenanceWindow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, Long> {
}
