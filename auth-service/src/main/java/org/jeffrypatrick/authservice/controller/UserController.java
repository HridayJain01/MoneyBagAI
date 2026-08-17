package org.jeffrypatrick.authservice.controller;

import org.jeffrypatrick.authservice.dto.UserInfoResponse;
import org.jeffrypatrick.authservice.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserInfoResponse me(Authentication authentication) {
        return userService.getCurrentUserInfo(authentication);
    }
}
