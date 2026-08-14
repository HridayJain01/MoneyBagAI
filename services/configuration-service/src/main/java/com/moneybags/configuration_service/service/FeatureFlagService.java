package com.moneybags.configuration_service.service;


import com.moneybags.configuration_service.dto.request.UpsertFeatureFlagRequest;
import com.moneybags.configuration_service.entity.FeatureFlag;
import com.moneybags.configuration_service.exception.NotFoundException;
import com.moneybags.configuration_service.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final AuditService auditService;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository, AuditService auditService) {
        this.featureFlagRepository = featureFlagRepository;
        this.auditService = auditService;
    }

    public List<FeatureFlag> getAll() {
        return featureFlagRepository.findAll();
    }

    public FeatureFlag getByKey(String flagKey) {
        return featureFlagRepository.findById(flagKey)
                .orElseThrow(() -> new NotFoundException("Feature flag not found: " + flagKey));
    }

    public FeatureFlag upsert(String flagKey, UpsertFeatureFlagRequest request) {
        FeatureFlag flag = featureFlagRepository.findById(flagKey).orElse(new FeatureFlag());
        String oldValue = flag.getFlagKey() == null ? null : describe(flag);
        flag.setFlagKey(flagKey);
        flag.setEnabled(request.getEnabled() ? "Y" : "N");
        flag.setDescription(request.getDescription());
        flag.setTargetingRule(request.getTargetingRule());
        flag.setUpdatedAt(LocalDateTime.now());
        FeatureFlag saved = featureFlagRepository.save(flag);
        auditService.logChange("FEATURE_FLAG", flagKey, oldValue, describe(saved));
        return saved;
    }

    public void delete(String flagKey) {
        FeatureFlag flag = getByKey(flagKey);
        String oldValue = describe(flag);
        featureFlagRepository.delete(flag);
        auditService.logChange("FEATURE_FLAG", flagKey, oldValue, null);
    }

    private String describe(FeatureFlag flag) {
        return "enabled=" + flag.getEnabled() + ", description=" + flag.getDescription();
    }
}
