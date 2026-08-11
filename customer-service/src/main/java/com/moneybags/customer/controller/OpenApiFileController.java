package com.moneybags.customer.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class OpenApiFileController {
    private static final MediaType YAML = MediaType.parseMediaType("application/yaml");

    @GetMapping(value = "/openapi.yml", produces = "application/yaml")
    public ResponseEntity<Resource> openApiFile() {
        return ResponseEntity.ok()
                .contentType(YAML)
                .body(new ClassPathResource("static/openapi.yml"));
    }
}
