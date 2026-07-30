package com.moneybags.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI productServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Moneybags Product Service API")
                .version("v1")
                .description("Product master and charge APIs"));
    }
}
