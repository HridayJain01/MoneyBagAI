package com.moneybags.configuration_service.service;

import com.moneybags.configuration_service.dto.request.CreateMakerCheckerThresholdRequest;
import com.moneybags.configuration_service.entity.MakerCheckerThreshold;
import com.moneybags.configuration_service.exception.NotFoundException;
import com.moneybags.configuration_service.repository.MakerCheckerThresholdRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MakerCheckerThresholdService {

    private final MakerCheckerThresholdRepository thresholdRepository;
    private final AuditService auditService;

    public MakerCheckerThresholdService(MakerCheckerThresholdRepository thresholdRepository, AuditService auditService) {
        this.thresholdRepository = thresholdRepository;
        this.auditService = auditService;
    }

    public List<MakerCheckerThreshold> getAllCurrent() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<MakerCheckerThreshold>> byActionType = thresholdRepository.findAll().stream()
                .filter(threshold -> !threshold.getEffectiveFrom().isAfter(now))
                .collect(Collectors.groupingBy(MakerCheckerThreshold::getActionType));

        return byActionType.values().stream()
                .map(this::latestEffective)
                .sorted(Comparator.comparing(MakerCheckerThreshold::getActionType))
                .toList();
    }

    public MakerCheckerThreshold getCurrent(String actionType) {
        LocalDateTime now = LocalDateTime.now();
        return thresholdRepository.findByActionType(actionType).stream()
                .filter(threshold -> !threshold.getEffectiveFrom().isAfter(now))
                .max(Comparator.comparing(MakerCheckerThreshold::getEffectiveFrom))
                .orElseThrow(() -> new NotFoundException(
                        "No effective maker-checker threshold configured for " + actionType));
    }

    public List<MakerCheckerThreshold> getHistory(String actionType) {
        return thresholdRepository.findByActionType(actionType).stream()
                .sorted(Comparator.comparing(MakerCheckerThreshold::getEffectiveFrom).reversed())
                .toList();
    }

    public MakerCheckerThreshold createVersion(CreateMakerCheckerThresholdRequest request) {
        MakerCheckerThreshold threshold = new MakerCheckerThreshold();
        threshold.setActionType(request.getActionType());
        threshold.setThresholdAmount(request.getThresholdAmount());
        threshold.setCurrency(defaultCurrency(request.getCurrency()));
        threshold.setEffectiveFrom(request.getEffectiveFrom() == null ? LocalDateTime.now() : request.getEffectiveFrom());
        MakerCheckerThreshold saved = thresholdRepository.save(threshold);
        auditService.logChange("MAKER_CHECKER_THRESHOLD", saved.getActionType(), null,
                "thresholdAmount=" + saved.getThresholdAmount() + ", currency=" + saved.getCurrency());
        return saved;
    }

    private MakerCheckerThreshold latestEffective(List<MakerCheckerThreshold> thresholds) {
        return thresholds.stream()
                .max(Comparator.comparing(MakerCheckerThreshold::getEffectiveFrom))
                .orElseThrow();
    }

    private String defaultCurrency(String currency) {
        return currency == null || currency.isBlank() ? "INR" : currency;
    }
}
