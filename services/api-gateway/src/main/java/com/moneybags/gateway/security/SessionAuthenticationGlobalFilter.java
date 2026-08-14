package com.moneybags.gateway.security;

import com.moneybags.gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Turns an opaque session id into the actor headers every downstream service expects.
 *
 * <p>Implemented as a {@link GlobalFilter} rather than a GatewayFilterFactory on
 * purpose: a filter factory has to be listed on each route, so adding a twelfth route
 * later would silently ship it unauthenticated. A global filter cannot be forgotten,
 * and the exemptions become one explicit allowlist that is worth reviewing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    /**
     * Headers a client must never be able to set for itself. Stripped unconditionally
     * on every request before anything else looks at them -- without this, a caller
     * simply sends "X-Permissions: TRANSACTION_APPROVE" and owns the bank.
     */
    private static final List<String> SPOOFABLE_ACTOR_HEADERS = List.of(
            "X-User-Id",
            "X-Employee-Id",
            "X-Branch-Code",
            "X-Branch-Id",
            "X-Permissions",
            "X-Customer-Id",
            "X-Service-Name");

    private final SessionResolver sessionResolver;
    private final GatewayProperties properties;

    @Override
    public int getOrder() {
        // Ahead of routing, and ahead of the audit filter at order 0.
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String correlationId = resolveCorrelationId(request);
        exchange.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);

        // Internal routes are service-to-service only. They carry no actor headers and
        // trust their caller, so they must never be reachable from outside.
        if (path.startsWith("/internal/")) {
            return reject(exchange, HttpStatus.FORBIDDEN, "INTERNAL_ROUTE_BLOCKED",
                    "Internal endpoints are not exposed through the gateway", correlationId);
        }

        if (isPublic(path)) {
            // Still stripped: a public route must not be a side door for actor headers
            // into a service that happens to read them.
            return chain.filter(exchange.mutate()
                    .request(stripActorHeaders(request).header(CORRELATION_HEADER, correlationId).build())
                    .build());
        }

        String sessionId = extractSessionId(request);
        if (sessionId == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "SESSION_REQUIRED",
                    "Provide a session as 'Authorization: Bearer <id>' or 'X-Session-Id'", correlationId);
        }

        return sessionResolver.resolve(sessionId)
                .flatMap(principal -> {
                    ServerHttpRequest mutated = stripActorHeaders(request)
                            .header(CORRELATION_HEADER, correlationId)
                            .header("X-User-Id", String.valueOf(principal.userId()))
                            .header("X-Permissions", String.join(",", principal.permissions()))
                            .headers(headers -> {
                                if (principal.employeeId() != null) {
                                    headers.set("X-Employee-Id", principal.employeeId());
                                }
                                if (principal.branchCode() != null) {
                                    // Same value under both names. transaction-service reads
                                    // X-Branch-Code, statement-reporting-service reads
                                    // X-Branch-Id; if they ever disagree, statement branch
                                    // scoping denies every request.
                                    headers.set("X-Branch-Code", principal.branchCode());
                                    headers.set("X-Branch-Id", principal.branchCode());
                                }
                            })
                            .build();

                    // Logging out must take effect now on this node, not after the cache TTL.
                    if (path.startsWith("/api/v1/auth/logout")) {
                        sessionResolver.evict(sessionId);
                    }
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .onErrorResume(SessionResolver.SessionRejectedException.class, ex ->
                        reject(exchange, HttpStatus.UNAUTHORIZED, "SESSION_INVALID",
                                ex.getMessage(), correlationId))
                .onErrorResume(SessionResolver.IdentityUnavailableException.class, ex ->
                        reject(exchange, HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_UNAVAILABLE",
                                ex.getMessage(), correlationId));
    }

    private ServerHttpRequest.Builder stripActorHeaders(ServerHttpRequest request) {
        ServerHttpRequest.Builder builder = request.mutate();
        SPOOFABLE_ACTOR_HEADERS.forEach(header -> builder.headers(h -> h.remove(header)));
        return builder;
    }

    private boolean isPublic(String path) {
        return properties.getPublicPaths().stream().anyMatch(path::startsWith);
    }

    private String extractSessionId(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String value = authorization.substring(7).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        String header = request.getHeaders().getFirst("X-Session-Id");
        return (header == null || header.isBlank()) ? null : header.trim();
    }

    private String resolveCorrelationId(ServerHttpRequest request) {
        String existing = request.getHeaders().getFirst(CORRELATION_HEADER);
        return (existing == null || existing.isBlank()) ? UUID.randomUUID().toString() : existing;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code,
                              String message, String correlationId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(CORRELATION_HEADER, correlationId);
        String body = """
                {"code":"%s","message":"%s","correlationId":"%s"}"""
                .formatted(code, message.replace("\"", "'"), correlationId);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
