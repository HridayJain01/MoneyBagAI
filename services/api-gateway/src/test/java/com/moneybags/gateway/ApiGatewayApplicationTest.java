package com.moneybags.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false"
})
@AutoConfigureWebTestClient
class ApiGatewayApplicationTest {

    @Autowired
    private WebTestClient client;

    @Test
    void contextLoads() {
    }

    @Test
    void acceptsTheAuthServiceAccessTokenCookie() {
        Instant now = Instant.now();
        String secret = "moneybags-dev-only-jwt-secret-change-before-production";
        String token = Jwts.builder()
                .issuer("moneybags-identity")
                .audience().add("moneybags-api").and()
                .subject("42")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .claim("username", "asha@example.com")
                .claim("roles", List.of("CUSTOMER"))
                .claim("permissions", List.of("ACCOUNT_VIEW"))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        int status = client.get().uri("/api/v1/users/me")
                .cookie("access-token", token)
                .exchange()
                .returnResult(Void.class)
                .getStatus()
                .value();

        // There is no registered security-service in this isolated test, so the
        // route may return 503. What matters is that authentication did not return 401.
        assertThat(status).isNotEqualTo(401);
    }
}
