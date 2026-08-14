package com.moneybags.identity.controller;

import com.moneybags.identity.api.ApiModels.*;
import com.moneybags.identity.service.AuthService;
import com.moneybags.identity.support.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @Operation(summary = "Authenticate and open a session")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return auth.login(request, clientIp(http), http.getHeader("User-Agent"));
    }

    @Operation(summary = "Revoke the current session")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http) {
        auth.logout(requireSession(http));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke every session for the current user")
    @PostMapping("/logout-all")
    public Map<String, Integer> logoutAll(HttpServletRequest http) {
        return Map.of("revoked", auth.logoutAll(requireSession(http)));
    }

    @Operation(summary = "List the current user's active sessions")
    @GetMapping("/sessions")
    public List<SessionDetail> sessions(HttpServletRequest http) {
        return auth.activeSessions(requireSession(http));
    }

    @Operation(summary = "Revoke one of the current user's sessions")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revoke(@PathVariable String sessionId, HttpServletRequest http) {
        auth.revokeSession(requireSession(http), sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Accepts the session from either the bearer header or X-Session-Id, matching what
     * the gateway itself accepts, so a client can use one convention everywhere.
     */
    private String requireSession(HttpServletRequest http) {
        String authorization = http.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        String header = http.getHeader("X-Session-Id");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        throw ApiException.unauthorized("SESSION_REQUIRED",
                "Provide the session as 'Authorization: Bearer <id>' or 'X-Session-Id'");
    }

    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }
}
