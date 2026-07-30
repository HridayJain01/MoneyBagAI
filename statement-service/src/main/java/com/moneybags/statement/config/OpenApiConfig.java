package com.moneybags.statement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI statementOpenApi() {
        return new OpenAPI().info(new Info().title("Moneybags Statement Service API").version("v1"));
    }
}
