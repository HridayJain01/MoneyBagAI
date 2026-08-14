package com.moneybags.configuration_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateChannelLimitRequest {

    @NotBlank(message = "channel is required")
    @Pattern(regexp = "UPI|NEFT|CASH|CHEQUE|CARD", message = "channel must be UPI, NEFT, CASH, CHEQUE, or CARD")
    private String channel;

    @NotBlank(message = "limitType is required")
    @Pattern(regexp = "PER_TXN|DAILY|MONTHLY", message = "limitType must be PER_TXN, DAILY, or MONTHLY")
    private String limitType;

    @NotNull(message = "maxAmount is required")
    @Positive(message = "maxAmount must be positive")
    private BigDecimal maxAmount;

    @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter uppercase code")
    private String currency;

    private LocalDateTime effectiveFrom;

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getLimitType() { return limitType; }
    public void setLimitType(String limitType) { this.limitType = limitType; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
}
