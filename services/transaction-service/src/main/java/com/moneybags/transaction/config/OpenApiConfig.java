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
                .description("Employee-operated APIs for deposits, withdrawals, transfers, transaction queries, approvals, reversals, and reconciliation. Use the Bearer JWT returned by /api/v1/auth/login.")
                .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearer()))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private SecurityScheme bearer() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT accessToken returned by POST /api/v1/auth/login");
    }
}
