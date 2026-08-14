package com.moneybags.customer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI customerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Moneybags Customer Service API")
                        .description("CIF, KYC, addresses, risk, communication preferences, nominees, beneficiaries and outbox events")
                        .version("v1"))
                .addServersItem(new Server().url("http://localhost:8090").description("API Gateway"))
                .addServersItem(new Server().url("http://localhost:8082").description("Customer Service directly"));
    }
}
