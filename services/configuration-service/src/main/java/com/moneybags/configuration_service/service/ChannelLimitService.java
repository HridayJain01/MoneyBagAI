package com.moneybags.configuration_service.service;

import com.moneybags.configuration_service.dto.request.CreateChannelLimitRequest;
import com.moneybags.configuration_service.entity.ChannelLimit;
import com.moneybags.configuration_service.exception.NotFoundException;
import com.moneybags.configuration_service.repository.ChannelLimitRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChannelLimitService {

    private final ChannelLimitRepository channelLimitRepository;
    private final AuditService auditService;

    public ChannelLimitService(ChannelLimitRepository channelLimitRepository, AuditService auditService) {
        this.channelLimitRepository = channelLimitRepository;
        this.auditService = auditService;
    }

    public List<ChannelLimit> getAllCurrent() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<ChannelLimit>> byChannelAndType = channelLimitRepository.findAll().stream()
                .filter(limit -> !limit.getEffectiveFrom().isAfter(now))
                .collect(Collectors.groupingBy(limit -> limit.getChannel() + "|" + limit.getLimitType()));

        return byChannelAndType.values().stream()
                .map(this::latestEffective)
                .sorted(Comparator.comparing(ChannelLimit::getChannel)
                        .thenComparing(ChannelLimit::getLimitType))
                .toList();
    }

    public ChannelLimit getCurrent(String channel, String limitType) {
        LocalDateTime now = LocalDateTime.now();
        return channelLimitRepository.findByChannelAndLimitType(channel, limitType).stream()
                .filter(limit -> !limit.getEffectiveFrom().isAfter(now))
                .max(Comparator.comparing(ChannelLimit::getEffectiveFrom))
                .orElseThrow(() -> new NotFoundException(
                        "No effective channel limit configured for " + channel + "/" + limitType));
    }

    public List<ChannelLimit> getHistory(String channel, String limitType) {
        return channelLimitRepository.findByChannelAndLimitType(channel, limitType).stream()
                .sorted(Comparator.comparing(ChannelLimit::getEffectiveFrom).reversed())
                .toList();
    }

    public ChannelLimit createVersion(CreateChannelLimitRequest request) {
        ChannelLimit limit = new ChannelLimit();
        limit.setChannel(request.getChannel());
        limit.setLimitType(request.getLimitType());
        limit.setMaxAmount(request.getMaxAmount());
        limit.setCurrency(defaultCurrency(request.getCurrency()));
        limit.setEffectiveFrom(request.getEffectiveFrom() == null ? LocalDateTime.now() : request.getEffectiveFrom());
        ChannelLimit saved = channelLimitRepository.save(limit);
        auditService.logChange("CHANNEL_LIMIT", saved.getChannel() + ":" + saved.getLimitType(), null,
                "maxAmount=" + saved.getMaxAmount() + ", currency=" + saved.getCurrency());
        return saved;
    }

    private ChannelLimit latestEffective(List<ChannelLimit> limits) {
        return limits.stream()
                .max(Comparator.comparing(ChannelLimit::getEffectiveFrom))
                .orElseThrow();
    }

    private String defaultCurrency(String currency) {
        return currency == null || currency.isBlank() ? "INR" : currency;
    }
}
