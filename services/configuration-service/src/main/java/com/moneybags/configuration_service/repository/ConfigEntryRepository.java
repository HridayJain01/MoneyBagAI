package com.moneybags.configuration_service.repository;

import com.moneybags.configuration_service.entity.ConfigEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigEntryRepository extends JpaRepository<ConfigEntry, Long> {
    List<ConfigEntry> findByNamespaceAndConfigKeyOrderByVersionDesc(String namespace, String configKey);
    List<ConfigEntry> findByNamespace(String namespace);
}
