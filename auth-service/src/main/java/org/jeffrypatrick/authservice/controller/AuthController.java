package org.jeffrypatrick.authservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.jeffrypatrick.authservice.dto.AuthResponse;
import org.jeffrypatrick.authservice.dto.LoginRequest;
import org.jeffrypatrick.authservice.dto.RegisterRequest;
import org.jeffrypatrick.authservice.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthService.LoginResult loginResult = authService.login(loginRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, loginResult.setCookieHeader())
                .body(loginResult.body());
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader(HttpHeaders.SET_COOKIE, authService.logoutCookie());
        return ResponseEntity.ok(new AuthResponse("Logged out successfully", null));
    }
}
