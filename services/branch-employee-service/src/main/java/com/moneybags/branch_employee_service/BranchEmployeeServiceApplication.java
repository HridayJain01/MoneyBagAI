package com.moneybags.branch_employee_service;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@OpenAPIDefinition(servers = @Server(url = "/", description = "MoneyBags API Gateway"))
public class BranchEmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BranchEmployeeServiceApplication.class, args);
    }
}
