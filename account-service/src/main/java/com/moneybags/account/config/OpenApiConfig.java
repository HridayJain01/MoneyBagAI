package com.moneybags.account.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI accountOpenApi() {
        return new OpenAPI().info(new Info().title("Moneybags Account Service API").version("v1"));
    }
}
