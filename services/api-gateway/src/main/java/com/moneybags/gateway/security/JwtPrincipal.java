package com.moneybags.gateway.security;

import java.util.List;

public record JwtPrincipal(
        Long userId,
        String username,
        String employeeId,
        String branchCode,
        List<String> roles,
        List<String> permissions) {
}
