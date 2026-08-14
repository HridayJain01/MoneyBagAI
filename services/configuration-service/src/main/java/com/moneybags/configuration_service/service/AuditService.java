package com.moneybags.configuration_service.service;

import com.moneybags.configuration_service.entity.ConfigChangeHistory;
import com.moneybags.configuration_service.repository.ConfigChangeHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final ConfigChangeHistoryRepository repository;

    public AuditService(ConfigChangeHistoryRepository repository) {
        this.repository = repository;
    }

    // TODO: populate changedBy once JWT/auth context is available.
    public void logChange(String entityType, String entityKey, String oldValue, String newValue) {
        ConfigChangeHistory history = new ConfigChangeHistory();
        history.setEntityType(entityType);
        history.setEntityKey(entityKey);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        repository.save(history);
    }
}
