package com.moneybags.transaction.security;

import com.moneybags.transaction.exception.DomainException;
import java.util.Set;

public record RequestActor(String userId,String customerId,String employeeId,String branchCode,Set<String> permissions,String correlationId) {
    public void require(String permission) {
        if (!permissions.contains(permission)) throw DomainException.forbidden("PERMISSION_DENIED","Missing permission: "+permission);
    }
    public String callerScope(){ return customerId != null ? "CUSTOMER:"+customerId : "USER:"+userId; }
}
