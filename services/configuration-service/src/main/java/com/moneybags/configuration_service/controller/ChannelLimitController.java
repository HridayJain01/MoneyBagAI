package com.moneybags.configuration_service.controller;

import com.moneybags.configuration_service.dto.request.CreateChannelLimitRequest;
import com.moneybags.configuration_service.entity.ChannelLimit;
import com.moneybags.configuration_service.service.ChannelLimitService;
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

@Tag(name = "Channel Limits", description = "Versioned transaction limits by channel")
@RestController
@RequestMapping("/api/v1/configuration/limits/channels")
public class ChannelLimitController {

    private final ChannelLimitService channelLimitService;

    public ChannelLimitController(ChannelLimitService channelLimitService) {
        this.channelLimitService = channelLimitService;
    }

    @GetMapping
    public List<ChannelLimit> getAllCurrent() {
        return channelLimitService.getAllCurrent();
    }

    @GetMapping("/{channel}/{limitType}")
    public ChannelLimit getCurrent(@PathVariable String channel, @PathVariable String limitType) {
        return channelLimitService.getCurrent(channel, limitType);
    }

    @GetMapping("/{channel}/{limitType}/history")
    public List<ChannelLimit> getHistory(@PathVariable String channel, @PathVariable String limitType) {
        return channelLimitService.getHistory(channel, limitType);
    }

    @PostMapping
    public ResponseEntity<ChannelLimit> createVersion(@Valid @RequestBody CreateChannelLimitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(channelLimitService.createVersion(request));
    }
}
