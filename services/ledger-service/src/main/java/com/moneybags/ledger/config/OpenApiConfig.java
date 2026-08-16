package com.moneybags.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI ledgerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MoneyBags Ledger Service API")
                .description("Auditable double-entry general ledger. Posted journals are immutable and corrections use reversals. Authenticate Swagger requests with the session ID returned by /api/v1/auth/login.")
                .version("v1"))
                .components(new Components().addSecuritySchemes("sessionId", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Session-Id")
                        .description("Raw sessionId returned by POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("sessionId"));
    }
}
