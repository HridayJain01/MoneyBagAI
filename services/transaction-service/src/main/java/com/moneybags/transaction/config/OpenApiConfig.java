package com.moneybags.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI transactionServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MoneyBags Transaction Service API")
                .description("Employee-operated APIs for deposits, withdrawals, transfers, transaction queries, approvals, reversals, and reconciliation. Authenticate Swagger requests with the session ID returned by /api/v1/auth/login.")
                .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("sessionId", header("X-Session-Id", "Raw sessionId returned by POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("sessionId"));
    }

    private SecurityScheme header(String name,String description) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(name).description(description);
    }
}
