package com.moneybags.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Provides useful browser landing paths for the otherwise API-only gateway. */
@RestController
public class GatewayLandingController {

    @GetMapping("/")
    public ResponseEntity<Void> gatewayHome() {
        return redirectToSwaggerUi();
    }

    @GetMapping("/swagger-ui/index.html")
    public ResponseEntity<Void> swaggerUiCompatibilityPath() {
        return redirectToSwaggerUi();
    }

    private ResponseEntity<Void> redirectToSwaggerUi() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/webjars/swagger-ui/index.html"))
                .build();
    }
}
