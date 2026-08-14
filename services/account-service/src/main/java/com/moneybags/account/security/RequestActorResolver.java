package com.moneybags.account.security;

import com.moneybags.account.support.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads the actor headers the gateway injects.
 *
 * <p>The gateway strips these headers from every inbound request before setting them
 * from a resolved session, so a client cannot forge them. This service must therefore
 * only ever be reachable through the gateway -- a direct call to :8083 with hand-written
 * headers would be trusted.
 *
 * <p>Applies to /api/v1 only. The /internal/v1 endpoints require no actor, because
 * transaction-service sends none.
 */
@Component
public class RequestActorResolver {

    public RequestActor resolve(HttpServletRequest request) {
        String employeeId = required(request, "X-Employee-Id");
        String branchCode = required(request, "X-Branch-Code");

        Set<String> permissions = Optional.ofNullable(request.getHeader("X-Permissions")).stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        String correlationId = Optional.ofNullable(request.getHeader("X-Correlation-Id"))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        return new RequestActor(employeeId, branchCode, permissions, correlationId);
    }

    private String required(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            // 403 rather than 401 to match transaction-service, so the gateway and the
            // services behind it report a missing actor the same way.
            throw ApiException.forbidden("AUTHENTICATION_REQUIRED", name + " is required");
        }
        return value;
    }
}
