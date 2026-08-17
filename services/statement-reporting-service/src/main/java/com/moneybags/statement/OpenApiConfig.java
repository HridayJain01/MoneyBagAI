package com.moneybags.statement;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI statementApi() {
        return new OpenAPI().info(new Info()
                .title("Moneybags Statement & Reporting Service")
                .version("v1")
                .description("Statements and reports projected from Account, Transaction, and Ledger services. Use the Bearer JWT returned by /api/v1/auth/login."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT accessToken returned by POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OperationCustomizer swaggerRequestHeaders() {
        return (operation, method) -> {
            if (method.getBeanType() == ProjectionController.class) return operation;
            header(operation,"X-Correlation-Id",false,"Request trace ID","swagger-test-001");
            return operation;
        };
    }

    private void header(Operation operation,String name,boolean required,String description,String example) {
        operation.addParametersItem(new Parameter().in("header").name(name).required(required)
                .description(description).schema(new StringSchema().example(example)));
    }
}
