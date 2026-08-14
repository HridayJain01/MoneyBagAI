package com.moneybags.configuration_service.controller;

import com.moneybags.configuration_service.dto.request.CreateOtpPolicyRequest;
import com.moneybags.configuration_service.dto.request.CreatePasswordPolicyRequest;
import com.moneybags.configuration_service.dto.request.CreateSessionPolicyRequest;
import com.moneybags.configuration_service.entity.OtpPolicy;
import com.moneybags.configuration_service.entity.PasswordPolicy;
import com.moneybags.configuration_service.entity.SessionPolicy;
import com.moneybags.configuration_service.service.PolicyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Policies", description = "Password, session, and OTP policies (versioned)")
@RestController
@RequestMapping("/api/v1/configuration/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/password")
    public PasswordPolicy getCurrentPassword() {
        return policyService.getCurrentPassword();
    }

    @GetMapping("/password/history")
    public List<PasswordPolicy> getPasswordHistory() {
        return policyService.getPasswordHistory();
    }

    @PostMapping("/password")
    public ResponseEntity<PasswordPolicy> createPasswordVersion(
            @Valid @RequestBody CreatePasswordPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createPasswordVersion(request));
    }

    @GetMapping("/session")
    public SessionPolicy getCurrentSession() {
        return policyService.getCurrentSession();
    }

    @GetMapping("/session/history")
    public List<SessionPolicy> getSessionHistory() {
        return policyService.getSessionHistory();
    }

    @PostMapping("/session")
    public ResponseEntity<SessionPolicy> createSessionVersion(
            @Valid @RequestBody CreateSessionPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createSessionVersion(request));
    }

    @GetMapping("/otp")
    public OtpPolicy getCurrentOtp() {
        return policyService.getCurrentOtp();
    }

    @GetMapping("/otp/history")
    public List<OtpPolicy> getOtpHistory() {
        return policyService.getOtpHistory();
    }

    @PostMapping("/otp")
    public ResponseEntity<OtpPolicy> createOtpVersion(@Valid @RequestBody CreateOtpPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createOtpVersion(request));
    }
}
