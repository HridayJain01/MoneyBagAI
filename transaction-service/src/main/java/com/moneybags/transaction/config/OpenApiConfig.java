package com.moneybags.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI transactionServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MoneyBags Transaction Service API")
                .description("APIs for deposits, withdrawals, transfers, transaction queries, approvals, reversals, and reconciliation.")
                .version("v1"));
    }
}
