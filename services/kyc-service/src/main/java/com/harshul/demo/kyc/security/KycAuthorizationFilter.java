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
import java.util.Set;

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

        String path = request.getRequestURI();
        boolean checkerDecision = path.matches(".*/sessions/[^/]+/(approve|reject)$");
        boolean tellerWrite = "POST".equalsIgnoreCase(request.getMethod());
        Set<String> accepted = checkerDecision
                ? Set.of("KYC_VERIFY")
                : tellerWrite ? Set.of("CUSTOMER_UPDATE") : Set.of("CUSTOMER_UPDATE", "KYC_VERIFY");

        String permissions = request.getHeader("X-Permissions");
        boolean allowed = permissions != null && Arrays.stream(permissions.split(","))
                .map(String::trim)
                .anyMatch(accepted::contains);
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String required = checkerDecision ? "KYC_VERIFY" : tellerWrite ? "CUSTOMER_UPDATE" : "CUSTOMER_UPDATE or KYC_VERIFY";
            response.getWriter().write("{\"code\":\"KYC_PERMISSION_REQUIRED\","
                    + "\"message\":\"" + required + " permission is required\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
