package com.moneybags.configuration_service.service;

import com.moneybags.configuration_service.dto.response.EffectiveConfigResponse;
import com.moneybags.configuration_service.entity.MaintenanceWindow;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EffectiveConfigService {

    private final FeatureFlagService featureFlagService;
    private final PolicyService policyService;
    private final ChannelLimitService channelLimitService;
    private final MakerCheckerThresholdService makerCheckerThresholdService;
    private final MaintenanceWindowService maintenanceWindowService;

    public EffectiveConfigService(FeatureFlagService featureFlagService,
                                  PolicyService policyService,
                                  ChannelLimitService channelLimitService,
                                  MakerCheckerThresholdService makerCheckerThresholdService,
                                  MaintenanceWindowService maintenanceWindowService) {
        this.featureFlagService = featureFlagService;
        this.policyService = policyService;
        this.channelLimitService = channelLimitService;
        this.makerCheckerThresholdService = makerCheckerThresholdService;
        this.maintenanceWindowService = maintenanceWindowService;
    }

    @Cacheable("effectiveConfig")
    public EffectiveConfigResponse getEffectiveConfig() {
        List<MaintenanceWindow> activeWindows = maintenanceWindowService.getAll().stream()
                .filter(window -> "ACTIVE".equals(window.getStatus()))
                .toList();
        return new EffectiveConfigResponse(
                policyService.getCurrentPassword(),
                policyService.getCurrentSession(),
                policyService.getCurrentOtp(),
                channelLimitService.getAllCurrent(),
                makerCheckerThresholdService.getAllCurrent(),
                featureFlagService.getAll(),
                activeWindows,
                LocalDateTime.now());
    }
}
