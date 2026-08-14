package com.moneybags.configuration_service.repository;

import com.moneybags.configuration_service.entity.ConfigChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigChangeHistoryRepository extends JpaRepository<ConfigChangeHistory, Long> {
    List<ConfigChangeHistory> findAll();
    List<ConfigChangeHistory> findByEntityType(String entityType);
    List<ConfigChangeHistory> findByEntityTypeAndEntityKey(String entityType, String entityKey);
}
