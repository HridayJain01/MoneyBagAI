package com.moneybags.statement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(StatementProperties.class)
public class StatementReportingApplication {
    public static void main(String[] args) { SpringApplication.run(StatementReportingApplication.class, args); }
}
