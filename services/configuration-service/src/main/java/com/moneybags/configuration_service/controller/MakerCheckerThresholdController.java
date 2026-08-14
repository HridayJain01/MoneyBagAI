package com.moneybags.configuration_service.controller;

import com.moneybags.configuration_service.dto.request.CreateMakerCheckerThresholdRequest;
import com.moneybags.configuration_service.entity.MakerCheckerThreshold;
import com.moneybags.configuration_service.service.MakerCheckerThresholdService;
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

@Tag(name = "Maker-Checker Thresholds", description = "Versioned approval thresholds by action")
@RestController
@RequestMapping("/api/v1/configuration/maker-checker-thresholds")
public class MakerCheckerThresholdController {

    private final MakerCheckerThresholdService thresholdService;

    public MakerCheckerThresholdController(MakerCheckerThresholdService thresholdService) {
        this.thresholdService = thresholdService;
    }

    @GetMapping
    public List<MakerCheckerThreshold> getAllCurrent() {
        return thresholdService.getAllCurrent();
    }

    @GetMapping("/{actionType}")
    public MakerCheckerThreshold getCurrent(@PathVariable String actionType) {
        return thresholdService.getCurrent(actionType);
    }

    @GetMapping("/{actionType}/history")
    public List<MakerCheckerThreshold> getHistory(@PathVariable String actionType) {
        return thresholdService.getHistory(actionType);
    }

    @PostMapping
    public ResponseEntity<MakerCheckerThreshold> createVersion(
            @Valid @RequestBody CreateMakerCheckerThresholdRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(thresholdService.createVersion(request));
    }
}
