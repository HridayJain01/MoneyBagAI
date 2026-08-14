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

    private String identityUri = "lb://security-service";
    private long sessionCacheTtlSeconds = 30;
    private long sessionCacheMaxSize = 10_000;
    private List<String> publicPaths = new ArrayList<>();
}
