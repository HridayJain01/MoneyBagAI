package com.moneybags.gateway.security;

import com.moneybags.gateway.config.GatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtTokenValidator {

    private final String issuer;
    private final String audience;
    private final SecretKey signingKey;

    public JwtTokenValidator(GatewayProperties gatewayProperties) {
        GatewayProperties.Jwt properties = gatewayProperties.getJwt();
        this.issuer = properties.getIssuer();
        this.audience = properties.getAudience();
        byte[] secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public JwtPrincipal validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
                throw new JwtRejectedException("JWT audience is not accepted");
            }

            return new JwtPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("employeeId", String.class),
                    claims.get("branchCode", String.class),
                    stringList(claims.get("roles")),
                    stringList(claims.get("permissions")));
        } catch (JwtRejectedException ex) {
            throw ex;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtRejectedException("JWT is invalid or expired");
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    public static class JwtRejectedException extends RuntimeException {
        public JwtRejectedException(String message) {
            super(message);
        }
    }
}
