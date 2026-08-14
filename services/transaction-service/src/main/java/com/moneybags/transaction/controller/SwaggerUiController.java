package com.moneybags.transaction.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerUiController {

    @GetMapping("/")
    String home() {
        return "redirect:/swagger-ui.html";
    }
}
