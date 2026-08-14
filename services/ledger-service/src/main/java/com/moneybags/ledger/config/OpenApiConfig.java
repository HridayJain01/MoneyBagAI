package com.moneybags.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI ledgerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("MoneyBags Ledger Service API")
                .description("Auditable double-entry general ledger. Posted journals are immutable and corrections use reversals.")
                .version("v1"));
    }
}
