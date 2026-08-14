package com.moneybags.configuration_service.dto.response;

import com.moneybags.configuration_service.entity.ChannelLimit;
import com.moneybags.configuration_service.entity.FeatureFlag;
import com.moneybags.configuration_service.entity.MaintenanceWindow;
import com.moneybags.configuration_service.entity.MakerCheckerThreshold;
import com.moneybags.configuration_service.entity.OtpPolicy;
import com.moneybags.configuration_service.entity.PasswordPolicy;
import com.moneybags.configuration_service.entity.SessionPolicy;

import java.time.LocalDateTime;
import java.util.List;

public class EffectiveConfigResponse {

    private final PasswordPolicy passwordPolicy;
    private final SessionPolicy sessionPolicy;
    private final OtpPolicy otpPolicy;
    private final List<ChannelLimit> channelLimits;
    private final List<MakerCheckerThreshold> makerCheckerThresholds;
    private final List<FeatureFlag> featureFlags;
    private final List<MaintenanceWindow> activeMaintenanceWindows;
    private final LocalDateTime generatedAt;

    public EffectiveConfigResponse(PasswordPolicy passwordPolicy, SessionPolicy sessionPolicy, OtpPolicy otpPolicy,
                                   List<ChannelLimit> channelLimits,
                                   List<MakerCheckerThreshold> makerCheckerThresholds,
                                   List<FeatureFlag> featureFlags,
                                   List<MaintenanceWindow> activeMaintenanceWindows,
                                   LocalDateTime generatedAt) {
        this.passwordPolicy = passwordPolicy;
        this.sessionPolicy = sessionPolicy;
        this.otpPolicy = otpPolicy;
        this.channelLimits = channelLimits;
        this.makerCheckerThresholds = makerCheckerThresholds;
        this.featureFlags = featureFlags;
        this.activeMaintenanceWindows = activeMaintenanceWindows;
        this.generatedAt = generatedAt;
    }

    public PasswordPolicy getPasswordPolicy() { return passwordPolicy; }
    public SessionPolicy getSessionPolicy() { return sessionPolicy; }
    public OtpPolicy getOtpPolicy() { return otpPolicy; }
    public List<ChannelLimit> getChannelLimits() { return channelLimits; }
    public List<MakerCheckerThreshold> getMakerCheckerThresholds() { return makerCheckerThresholds; }
    public List<FeatureFlag> getFeatureFlags() { return featureFlags; }
    public List<MaintenanceWindow> getActiveMaintenanceWindows() { return activeMaintenanceWindows; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
