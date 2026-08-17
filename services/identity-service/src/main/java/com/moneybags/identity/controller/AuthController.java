package com.moneybags.identity.controller;

import com.moneybags.identity.api.ApiModels.LoginRequest;
import com.moneybags.identity.api.ApiModels.LoginResponse;
import com.moneybags.identity.api.ApiModels.RegisterRequest;
import com.moneybags.identity.api.ApiModels.RegistrationResponse;
import com.moneybags.identity.config.IdentityConfig.IdentityProperties;
import com.moneybags.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final IdentityProperties properties;

    @Operation(summary = "Register a customer identity")
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }

    @Operation(summary = "Authenticate and issue a JWT access token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest http) {
        LoginResponse response = auth.login(request, clientIp(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie(response.accessToken(), response.expiresAt()))
                .body(response);
    }

    @Operation(summary = "Record logout; the client must discard its stateless JWT")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-User-Id") Long userId, HttpServletRequest http) {
        auth.recordLogout(userId, clientIp(http), http.getHeader("User-Agent"));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearAccessTokenCookie())
                .build();
    }

    private String accessTokenCookie(String token, Instant expiresAt) {
        Duration lifetime = Duration.between(Instant.now(), expiresAt);
        return ResponseCookie.from(properties.getCookie().getName(), token)
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite())
                .path("/")
                .maxAge(lifetime.isNegative() ? Duration.ZERO : lifetime)
                .build()
                .toString();
    }

    private String clearAccessTokenCookie() {
        return ResponseCookie.from(properties.getCookie().getName(), "")
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }

    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
