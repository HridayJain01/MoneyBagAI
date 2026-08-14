package com.moneybags.statement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(StatementProperties.class)
@OpenAPIDefinition(servers = @Server(url = "/", description = "MoneyBags API Gateway"))
public class StatementReportingApplication {
    public static void main(String[] args) { SpringApplication.run(StatementReportingApplication.class, args); }
}
