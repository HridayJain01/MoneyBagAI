package com.moneybags.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * Reactive and load-balanced, so {@code lb://security-service} resolves through
     * Eureka.
     *
     * <p>This must never become a {@code RestTemplate} or a Feign client: the gateway
     * runs on Netty, and a blocking call here stalls the event loop for every
     * concurrent request, not just this one.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
