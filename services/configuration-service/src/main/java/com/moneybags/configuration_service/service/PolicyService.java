package com.moneybags.configuration_service.service;

import com.moneybags.configuration_service.dto.request.CreateOtpPolicyRequest;
import com.moneybags.configuration_service.dto.request.CreatePasswordPolicyRequest;
import com.moneybags.configuration_service.dto.request.CreateSessionPolicyRequest;
import com.moneybags.configuration_service.entity.OtpPolicy;
import com.moneybags.configuration_service.entity.PasswordPolicy;
import com.moneybags.configuration_service.entity.SessionPolicy;
import com.moneybags.configuration_service.exception.NotFoundException;
import com.moneybags.configuration_service.repository.OtpPolicyRepository;
import com.moneybags.configuration_service.repository.PasswordPolicyRepository;
import com.moneybags.configuration_service.repository.SessionPolicyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PolicyService {

    private final PasswordPolicyRepository passwordPolicyRepository;
    private final SessionPolicyRepository sessionPolicyRepository;
    private final OtpPolicyRepository otpPolicyRepository;
    private final AuditService auditService;

    public PolicyService(PasswordPolicyRepository passwordPolicyRepository,
                         SessionPolicyRepository sessionPolicyRepository,
                         OtpPolicyRepository otpPolicyRepository, AuditService auditService) {
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.sessionPolicyRepository = sessionPolicyRepository;
        this.otpPolicyRepository = otpPolicyRepository;
        this.auditService = auditService;
    }

    public PasswordPolicy getCurrentPassword() {
        return passwordPolicyRepository
                .findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDateTime.now())
                .orElseThrow(() -> new NotFoundException("No effective password policy configured"));
    }

    public SessionPolicy getCurrentSession() {
        return sessionPolicyRepository
                .findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDateTime.now())
                .orElseThrow(() -> new NotFoundException("No effective session policy configured"));
    }

    public OtpPolicy getCurrentOtp() {
        return otpPolicyRepository
                .findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDateTime.now())
                .orElseThrow(() -> new NotFoundException("No effective otp policy configured"));
    }

    public List<PasswordPolicy> getPasswordHistory() {
        return passwordPolicyRepository.findAllByOrderByEffectiveFromDesc();
    }

    public List<SessionPolicy> getSessionHistory() {
        return sessionPolicyRepository.findAllByOrderByEffectiveFromDesc();
    }

    public List<OtpPolicy> getOtpHistory() {
        return otpPolicyRepository.findAllByOrderByEffectiveFromDesc();
    }

    public PasswordPolicy createPasswordVersion(CreatePasswordPolicyRequest request) {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setMinLength(request.getMinLength());
        policy.setRequireUpper(toYn(request.getRequireUpper()));
        policy.setRequireDigit(toYn(request.getRequireDigit()));
        policy.setRequireSpecial(toYn(request.getRequireSpecial()));
        policy.setHistoryCount(request.getHistoryCount());
        policy.setMaxAgeDays(request.getMaxAgeDays());
        policy.setEffectiveFrom(effectiveFromOrNow(request.getEffectiveFrom()));
        PasswordPolicy saved = passwordPolicyRepository.save(policy);
        auditService.logChange("PASSWORD_POLICY", String.valueOf(saved.getId()), null,
                "minLength=" + saved.getMinLength() + ", historyCount=" + saved.getHistoryCount());
        return saved;
    }

    public SessionPolicy createSessionVersion(CreateSessionPolicyRequest request) {
        SessionPolicy policy = new SessionPolicy();
        policy.setIdleTimeoutMinutes(request.getIdleTimeoutMinutes());
        policy.setAbsoluteTimeoutMinutes(request.getAbsoluteTimeoutMinutes());
        policy.setMaxConcurrentSessions(request.getMaxConcurrentSessions());
        policy.setEffectiveFrom(effectiveFromOrNow(request.getEffectiveFrom()));
        SessionPolicy saved = sessionPolicyRepository.save(policy);
        auditService.logChange("SESSION_POLICY", String.valueOf(saved.getId()), null,
                "idleTimeoutMinutes=" + saved.getIdleTimeoutMinutes());
        return saved;
    }

    public OtpPolicy createOtpVersion(CreateOtpPolicyRequest request) {
        OtpPolicy policy = new OtpPolicy();
        policy.setExpirySeconds(request.getExpirySeconds());
        policy.setMaxRetries(request.getMaxRetries());
        policy.setResendCooldownSeconds(request.getResendCooldownSeconds());
        policy.setEffectiveFrom(effectiveFromOrNow(request.getEffectiveFrom()));
        OtpPolicy saved = otpPolicyRepository.save(policy);
        auditService.logChange("OTP_POLICY", String.valueOf(saved.getId()), null,
                "expirySeconds=" + saved.getExpirySeconds() + ", maxRetries=" + saved.getMaxRetries());
        return saved;
    }

    private String toYn(Boolean value) {
        return Boolean.FALSE.equals(value) ? "N" : "Y";
    }

    private LocalDateTime effectiveFromOrNow(LocalDateTime effectiveFrom) {
        return effectiveFrom == null ? LocalDateTime.now() : effectiveFrom;
    }
}
