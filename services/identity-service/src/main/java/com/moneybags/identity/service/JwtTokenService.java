package com.moneybags.identity.service;

import com.moneybags.identity.config.IdentityConfig.IdentityProperties;
import com.moneybags.identity.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {

    private final IdentityProperties.Jwt properties;
    private final SecretKey signingKey;

    public JwtTokenService(IdentityProperties identityProperties) {
        this.properties = identityProperties.getJwt();
        byte[] secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public IssuedToken issue(User user, List<String> roles, List<String> permissions) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.getExpirationMinutes(), ChronoUnit.MINUTES);

        String token = Jwts.builder()
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("username", user.getUsername())
                .claim("employeeId", user.getEmployeeId())
                .claim("branchCode", user.getBranchCode())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .signWith(signingKey)
                .compact();

        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
