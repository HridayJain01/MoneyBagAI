package com.moneybags.branch_employee_service.service;

import com.moneybags.branch_employee_service.client.SecurityClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over {@link SecurityClient} so the calling service keeps a boolean-shaped
 * dependency and does not have to know about Feign exception types.
 */
@Component
public class IdentityClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityClient.class);

    private final SecurityClient securityClient;

    public IdentityClient(SecurityClient securityClient) {
        this.securityClient = securityClient;
    }

    public boolean userExists(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            return securityClient.findUser(userId) != null;
        } catch (Exception ex) {
            // Covers both "no such user" (404) and identity being down. Callers treat
            // this as "cannot confirm", which correctly blocks creating an employee
            // against an unverifiable user.
            log.warn("Could not verify identity user {}: {}", userId, ex.getMessage());
            return false;
        }
    }

    /**
     * Best-effort. A failure here must not roll back employee creation -- the employment
     * link can be repaired by calling identity directly, whereas a half-created employee
     * cannot.
     */
    public void publishEmployment(Long userId, Long employeeId, String branchCode) {
        try {
            securityClient.setEmployment(userId,
                    new SecurityClient.EmploymentRequest(String.valueOf(employeeId), branchCode));
        } catch (Exception ex) {
            log.warn("Could not push employment for user {} (employee {}, branch {}): {}",
                    userId, employeeId, branchCode, ex.getMessage());
        }
    }
}
