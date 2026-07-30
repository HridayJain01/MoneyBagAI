package com.moneybags.security.controller;

import com.moneybags.security.dto.*;
import com.moneybags.security.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @PostMapping
    ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
    @GetMapping("/{userId}")
    UserResponse findById(@PathVariable Long userId) { return service.findById(userId); }
    @GetMapping
    List<UserResponse> findAll() { return service.findAll(); }
}
