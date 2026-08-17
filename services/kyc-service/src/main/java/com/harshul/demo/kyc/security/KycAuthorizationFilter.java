package com.harshul.demo.kyc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class KycAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/kyc/")
                && !request.getRequestURI().equals("/api/v1/kyc");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String permissions = request.getHeader("X-Permissions");
        boolean allowed = permissions != null && Arrays.stream(permissions.split(","))
                .map(String::trim)
                .anyMatch("KYC_VERIFY"::equals);
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"KYC_PERMISSION_REQUIRED\","
                    + "\"message\":\"KYC_VERIFY permission is required\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
