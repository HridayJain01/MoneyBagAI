package com.moneybags.account.security;

import com.moneybags.account.support.ApiException;

import java.util.Set;

/**
 * The authenticated employee behind a public request, as injected by the gateway.
 *
 * <p>Mirrors transaction-service's RequestActor so the two services agree on header
 * names and on what a permission denial looks like.
 */
public record RequestActor(String employeeId, String branchCode, Set<String> permissions,
                           String correlationId) {

    public static final String PERMISSION_VIEW = "ACCOUNT_VIEW";
    public static final String PERMISSION_VIEW_ALL_BRANCHES = "ACCOUNT_VIEW_ALL_BRANCHES";
    public static final String PERMISSION_OPEN = "ACCOUNT_OPEN";
    public static final String PERMISSION_APPROVE = "ACCOUNT_APPROVE";
    public static final String PERMISSION_STATUS_MANAGE = "ACCOUNT_STATUS_MANAGE";

    public void require(String permission) {
        if (!permissions.contains(permission)) {
            throw ApiException.forbidden("PERMISSION_DENIED", "Missing permission: " + permission);
        }
    }

    public boolean canAccessAllBranches() {
        return permissions.contains(PERMISSION_VIEW_ALL_BRANCHES);
    }

    /** Staff are branch-scoped unless they hold the cross-branch permission. */
    public void requireBranchAccess(String branchCode) {
        if (canAccessAllBranches()) {
            return;
        }
        if (!this.branchCode.equals(branchCode)) {
            throw ApiException.forbidden("BRANCH_SCOPE_DENIED",
                    "This resource belongs to branch " + branchCode);
        }
    }
}
