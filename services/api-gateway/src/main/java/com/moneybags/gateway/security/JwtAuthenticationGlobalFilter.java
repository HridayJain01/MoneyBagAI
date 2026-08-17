package com.moneybags.gateway.security;

import com.moneybags.gateway.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
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

/** Verifies identity-service JWTs and injects trusted actor headers downstream. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static final List<String> SPOOFABLE_ACTOR_HEADERS = List.of(
            "X-User-Id", "X-Employee-Id", "X-Branch-Code", "X-Branch-Id",
            "X-Permissions", "X-Customer-Id", "X-Service-Name");

    private final JwtTokenValidator tokenValidator;
    private final GatewayProperties properties;

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String correlationId = resolveCorrelationId(request);
        exchange.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);

        if (path.startsWith("/internal/")) {
            return reject(exchange, HttpStatus.FORBIDDEN, "INTERNAL_ROUTE_BLOCKED",
                    "Internal endpoints are not exposed through the gateway", correlationId);
        }

        if (isPublic(path)) {
            return chain.filter(exchange.mutate()
                    .request(stripActorHeaders(request).header(CORRELATION_HEADER, correlationId).build())
                    .build());
        }

        String token = extractAccessToken(request);
        if (token == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "JWT_REQUIRED",
                    "Provide a JWT as 'Authorization: Bearer <token>' or in the access-token cookie",
                    correlationId);
        }

        final JwtPrincipal principal;
        try {
            principal = tokenValidator.validate(token);
        } catch (JwtTokenValidator.JwtRejectedException ex) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "JWT_INVALID", ex.getMessage(), correlationId);
        }

        ServerHttpRequest mutated = stripActorHeaders(request)
                .header(CORRELATION_HEADER, correlationId)
                .header("X-User-Id", String.valueOf(principal.userId()))
                .header("X-Permissions", String.join(",", principal.permissions()))
                .headers(headers -> {
                    if (principal.employeeId() != null) {
                        headers.set("X-Employee-Id", principal.employeeId());
                    }
                    if (principal.branchCode() != null) {
                        headers.set("X-Branch-Code", principal.branchCode());
                        headers.set("X-Branch-Id", principal.branchCode());
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private ServerHttpRequest.Builder stripActorHeaders(ServerHttpRequest request) {
        ServerHttpRequest.Builder builder = request.mutate();
        SPOOFABLE_ACTOR_HEADERS.forEach(header -> builder.headers(h -> h.remove(header)));
        return builder;
    }

    private boolean isPublic(String path) {
        return properties.getPublicPaths().stream().anyMatch(path::startsWith);
    }

    private String extractAccessToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authorization.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        var cookie = request.getCookies().getFirst(properties.getAccessTokenCookieName());
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
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
