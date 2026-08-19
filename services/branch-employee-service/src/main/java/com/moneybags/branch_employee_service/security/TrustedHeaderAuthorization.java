package com.moneybags.branch_employee_service.security;

import com.moneybags.branch_employee_service.exception.ForbiddenException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Enforces the permission list injected by the API gateway after JWT validation.
 * Caller-supplied actor headers are stripped by the gateway before this service
 * receives the request.
 */
@Component
public class TrustedHeaderAuthorization {

    public void require(String permissionHeader, String requiredPermission) {
        boolean allowed = permissionHeader != null && Arrays.stream(permissionHeader.split(","))
                .map(String::trim)
                .anyMatch(requiredPermission::equals);
        if (!allowed) {
            throw new ForbiddenException(requiredPermission + " permission is required");
        }
    }
}
