package com.moneybags.identity.security;

import com.moneybags.identity.support.ApiException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Authorization for gateway-facing identity administration endpoints. The API
 * gateway strips caller-supplied actor headers, validates the JWT, and injects
 * this trusted permission list.
 */
@Component
public class TrustedHeaderAuthorization {

    public void require(String permissionHeader, String requiredPermission) {
        boolean allowed = permissionHeader != null && Arrays.stream(permissionHeader.split(","))
                .map(String::trim)
                .anyMatch(requiredPermission::equals);
        if (!allowed) {
            throw ApiException.forbidden("PERMISSION_REQUIRED",
                    requiredPermission + " permission is required");
        }
    }
}
