package com.moneybags.identity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class IdentityConfig {

    /**
     * BCrypt from spring-security-crypto only. Deliberately NOT
     * spring-boot-starter-security, which would install a servlet filter chain that
     * nothing in this architecture wants -- authorisation happens at the gateway.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public OpenAPI identityOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MoneyBags Identity Service")
                .version("v1")
                .description("Users, roles, permissions and JWT authentication."));
    }

    @Getter
    @Setter
    @Configuration
    @ConfigurationProperties(prefix = "moneybags.identity")
    public static class IdentityProperties {
        private int maxFailedAttempts = 5;
        private long lockDurationMinutes = 15;
        private Jwt jwt = new Jwt();

        @Getter
        @Setter
        public static class Jwt {
            private String secret = "moneybags-dev-only-jwt-secret-change-before-production";
            private long expirationMinutes = 15;
            private String issuer = "moneybags-identity";
            private String audience = "moneybags-api";
        }
    }

    /**
     * The Eureka application name must be {@code security-service}: customer-service's
     * {@code SecurityClient} resolves that id, not the module name. Assert it loudly
     * so a rename is caught at boot rather than as a mystery 500 later.
     */
    @Slf4j
    @Configuration
    public static class RegistrationNameCheck {
        @Value("${spring.application.name}")
        private String applicationName;

        @EventListener(ApplicationReadyEvent.class)
        public void verify() {
            if (!"security-service".equals(applicationName)) {
                log.error("spring.application.name is '{}' but customer-service's SecurityClient "
                        + "resolves the Eureka id 'security-service'. User lookups will fail.", applicationName);
            } else {
                log.info("Registered with Eureka as 'security-service' (module identity-service).");
            }
        }
    }
}
