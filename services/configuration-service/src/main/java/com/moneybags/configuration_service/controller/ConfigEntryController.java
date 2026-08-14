package com.moneybags.configuration_service.controller;

import com.moneybags.configuration_service.dto.request.CreateConfigEntryRequest;
import com.moneybags.configuration_service.entity.ConfigEntry;
import com.moneybags.configuration_service.service.ConfigEntryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Config Entries", description = "Versioned typed configuration entries")
@RestController
@RequestMapping("/api/v1/configuration/entries")
public class ConfigEntryController {

    private final ConfigEntryService configEntryService;

    public ConfigEntryController(ConfigEntryService configEntryService) {
        this.configEntryService = configEntryService;
    }

    @GetMapping("/{namespace}/{configKey}")
    public ConfigEntry getCurrent(@PathVariable String namespace, @PathVariable String configKey) {
        return configEntryService.getCurrent(namespace, configKey);
    }

    @GetMapping("/{namespace}/{configKey}/history")
    public List<ConfigEntry> getHistory(@PathVariable String namespace, @PathVariable String configKey) {
        return configEntryService.getHistory(namespace, configKey);
    }

    @GetMapping("/{namespace}")
    public List<ConfigEntry> getByNamespace(@PathVariable String namespace) {
        return configEntryService.getByNamespace(namespace);
    }

    @PostMapping
    public ResponseEntity<ConfigEntry> createVersion(@Valid @RequestBody CreateConfigEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(configEntryService.createVersion(request));
    }
}
