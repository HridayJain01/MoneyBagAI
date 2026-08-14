package com.moneybags.configuration_service.service;

import com.moneybags.configuration_service.dto.request.CreateConfigEntryRequest;
import com.moneybags.configuration_service.entity.ConfigEntry;
import com.moneybags.configuration_service.exception.NotFoundException;
import com.moneybags.configuration_service.repository.ConfigEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConfigEntryService {

    private final ConfigEntryRepository configEntryRepository;
    private final AuditService auditService;

    public ConfigEntryService(ConfigEntryRepository configEntryRepository, AuditService auditService) {
        this.configEntryRepository = configEntryRepository;
        this.auditService = auditService;
    }

    public ConfigEntry getCurrent(String namespace, String configKey) {
        LocalDateTime now = LocalDateTime.now();
        return configEntryRepository.findByNamespaceAndConfigKeyOrderByVersionDesc(namespace, configKey).stream()
                .filter(entry -> !entry.getEffectiveFrom().isAfter(now))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No effective config entry configured for " + namespace + "/" + configKey));
    }

    public List<ConfigEntry> getHistory(String namespace, String configKey) {
        return configEntryRepository.findByNamespaceAndConfigKeyOrderByVersionDesc(namespace, configKey);
    }

    public List<ConfigEntry> getByNamespace(String namespace) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<ConfigEntry>> byConfigKey = configEntryRepository.findByNamespace(namespace).stream()
                .filter(entry -> !entry.getEffectiveFrom().isAfter(now))
                .collect(Collectors.groupingBy(ConfigEntry::getConfigKey));

        return byConfigKey.values().stream()
                .map(entries -> entries.stream()
                        .max(Comparator.comparing(ConfigEntry::getVersion))
                        .orElseThrow())
                .sorted(Comparator.comparing(ConfigEntry::getConfigKey))
                .toList();
    }

    public ConfigEntry createVersion(CreateConfigEntryRequest request) {
        List<ConfigEntry> history = configEntryRepository
                .findByNamespaceAndConfigKeyOrderByVersionDesc(request.getNamespace(), request.getConfigKey());
        int nextVersion = history.isEmpty() ? 1 : history.get(0).getVersion() + 1;
        LocalDateTime now = LocalDateTime.now();

        ConfigEntry entry = new ConfigEntry();
        entry.setNamespace(request.getNamespace());
        entry.setConfigKey(request.getConfigKey());
        entry.setConfigValue(request.getConfigValue());
        entry.setValueType(request.getValueType());
        entry.setVersion(nextVersion);
        entry.setEffectiveFrom(now);
        entry.setEffectiveTo(null);
        entry.setUpdatedBy(request.getUpdatedBy());
        entry.setUpdatedAt(now);
        ConfigEntry saved = configEntryRepository.save(entry);
        auditService.logChange("CONFIG_ENTRY", saved.getNamespace() + ":" + saved.getConfigKey(), null,
                "version=" + saved.getVersion() + ", valueType=" + saved.getValueType());
        return saved;
    }
}
