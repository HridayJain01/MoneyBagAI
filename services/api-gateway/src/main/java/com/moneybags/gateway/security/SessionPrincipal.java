package com.moneybags.gateway.security;

import java.time.Instant;
import java.util.List;

/**
 * Mirrors identity-service's {@code ApiModels.SessionPrincipal}. Kept as a local
 * record rather than a shared module so the gateway has no compile dependency on
 * another service's jar.
 */
public record SessionPrincipal(
        Long userId,
        String username,
        String employeeId,
        String branchCode,
        List<String> permissions,
        String status,
        Instant expiresAt) {
}
