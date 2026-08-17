package com.moneybags.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component("moneyBagsGatewayProperties")
@ConfigurationProperties(prefix = "moneybags.gateway")
public class GatewayProperties {

    private Jwt jwt = new Jwt();
    private List<String> publicPaths = new ArrayList<>();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "moneybags-dev-only-jwt-secret-change-before-production";
        private String issuer = "moneybags-identity";
        private String audience = "moneybags-api";
    }
}
