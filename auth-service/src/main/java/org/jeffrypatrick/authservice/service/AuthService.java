package org.jeffrypatrick.authservice.service;

import org.jeffrypatrick.authservice.dto.AuthResponse;
import org.jeffrypatrick.authservice.dto.LoginRequest;
import org.jeffrypatrick.authservice.dto.RegisterRequest;
import org.jeffrypatrick.authservice.dto.UserInfoResponse;
import org.jeffrypatrick.authservice.model.*;
import org.jeffrypatrick.authservice.repository.RoleRepository;
import org.jeffrypatrick.authservice.repository.UserRepository;
import org.jeffrypatrick.authservice.utility.JwtUtil;
import org.jeffrypatrick.authservice.utility.UserUtil;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;

@Service
public class AuthService {

    public static final String ACCESS_TOKEN_COOKIE = "access-token";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role defaultRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Default CUSTOMER role not found"));

//        User user = new User();
//        user.setFirstName(request.firstName());
//        user.setLastName(request.lastName());
//        user.setEmail(normalizedEmail);
//        user.setPasswordHash(passwordEncoder.encode(request.password()));
//        user.setDob(request.dob());
//        user.setGender(request.gender());
//        user.setMobile(request.mobile());
//        user.setStatus(Status.ACTIVE);
//        user.setRole(defaultRole);

        User saved = userRepository.save(
                UserUtil.commonCreateUser(
                        request.firstName(),
                        request.lastName(),
                        normalizedEmail,
                        passwordEncoder.encode(request.password()),
                        request.dob(),
                        request.gender(),
                        request.mobile(),
                        Status.ACTIVE,
                        defaultRole
                )
        );

        return new AuthResponse(
                "Registered successfully",
                toUserInfoResponse(saved)
        );
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        String normalizedEmail = request.email().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(normalizedEmail);
        String jwt = jwtUtil.generateAccessToken(userDetails);

        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, jwt)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(jwtUtil.getAccessTokenExpirationMs()))
                .build();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("User not found after login"));

        return new LoginResult(
                jwt,
                cookie.toString(),
                new AuthResponse("Login successful", toUserInfoResponse(user))
        );
    }

    public String logoutCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build()
                .toString();
    }

    private UserInfoResponse toUserInfoResponse(User user) {
        Set<String> permissions = user.getRole() != null
                ? user.getRole().getPermissions().stream()
                .map(Permission::getName)
                .collect(java.util.stream.Collectors.toSet())
                : Set.of();

        return new UserInfoResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getDob(),
                user.getGender(),
                user.getMobile(),
                user.getRole() != null ? user.getRole().getName().name() : null,
                permissions,
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    public record LoginResult(String token, String setCookieHeader, AuthResponse body) {
    }
}