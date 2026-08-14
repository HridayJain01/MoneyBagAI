package com.moneybags.gateway.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.moneybags.gateway.config.GatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Resolves an opaque session id to its principal by calling identity-service, with a
 * short-TTL cache in front so a busy request path does not hammer identity.
 *
 * <p>The cache TTL is the propagation delay for a logout or a role change. 30s is the
 * deliberate trade at this scale; {@link #evict(String)} makes logout immediate on the
 * node that handled it.
 */
@Slf4j
@Component
public class SessionResolver {

    private final WebClient webClient;
    private final Cache<String, SessionPrincipal> cache;

    public SessionResolver(WebClient.Builder loadBalancedWebClientBuilder, GatewayProperties properties) {
        this.webClient = loadBalancedWebClientBuilder.baseUrl(properties.getIdentityUri()).build();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getSessionCacheTtlSeconds()))
                .maximumSize(properties.getSessionCacheMaxSize())
                .build();
    }

    public Mono<SessionPrincipal> resolve(String sessionId) {
        SessionPrincipal cached = cache.getIfPresent(sessionId);
        if (cached != null) {
            return Mono.just(cached);
        }
        return webClient.post()
                .uri("/internal/v1/sessions/resolve")
                .bodyValue(Map.of("sessionId", sessionId))
                .retrieve()
                .bodyToMono(SessionPrincipal.class)
                .doOnNext(principal -> cache.put(sessionId, principal))
                .onErrorMap(WebClientResponseException.class, this::translate);
    }

    public void evict(String sessionId) {
        cache.invalidate(sessionId);
    }

    /**
     * A rejected session is the caller's problem (401); anything else is identity
     * being unavailable, which is ours (503). Collapsing both into 401 would send
     * users to re-login during an identity outage.
     */
    private Throwable translate(WebClientResponseException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN
                || status == HttpStatus.NOT_FOUND) {
            return new SessionRejectedException("Session is not valid");
        }
        log.warn("Identity service returned {} resolving a session", ex.getStatusCode());
        return new IdentityUnavailableException("Identity service is unavailable");
    }

    public static class SessionRejectedException extends RuntimeException {
        public SessionRejectedException(String message) {
            super(message);
        }
    }

    public static class IdentityUnavailableException extends RuntimeException {
        public IdentityUnavailableException(String message) {
            super(message);
        }
    }
}
