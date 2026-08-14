package com.moneybags.configuration_service.controller;


import com.moneybags.configuration_service.dto.request.UpsertFeatureFlagRequest;
import com.moneybags.configuration_service.entity.FeatureFlag;
import com.moneybags.configuration_service.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Feature Flags", description = "Toggleable feature flags")
@RestController
@RequestMapping("/api/v1/configuration/feature-flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public List<FeatureFlag> getAll() {
        return featureFlagService.getAll();
    }

    @GetMapping("/{flagKey}")
    public FeatureFlag getByKey(@PathVariable String flagKey) {
        return featureFlagService.getByKey(flagKey);
    }

    @PutMapping("/{flagKey}")
    public FeatureFlag upsert(@PathVariable String flagKey,
                               @Valid @RequestBody UpsertFeatureFlagRequest request) {
        return featureFlagService.upsert(flagKey, request);
    }

    @DeleteMapping("/{flagKey}")
    public ResponseEntity<Void> delete(@PathVariable String flagKey) {
        featureFlagService.delete(flagKey);
        return ResponseEntity.noContent().build();
    }
}
