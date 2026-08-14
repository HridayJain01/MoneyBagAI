package com.moneybags.configuration_service.controller;

import com.moneybags.configuration_service.dto.response.EffectiveConfigResponse;
import com.moneybags.configuration_service.service.EffectiveConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/configuration")
public class InternalEffectiveConfigController {

    private final EffectiveConfigService effectiveConfigService;

    public InternalEffectiveConfigController(EffectiveConfigService effectiveConfigService) {
        this.effectiveConfigService = effectiveConfigService;
    }

    @GetMapping("/effective")
    public EffectiveConfigResponse getEffective() {
        return effectiveConfigService.getEffectiveConfig();
    }
}
