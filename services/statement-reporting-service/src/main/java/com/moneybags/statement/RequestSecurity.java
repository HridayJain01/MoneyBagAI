package com.moneybags.statement;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

record Actor(String userId,String cif,String employeeId,String branchId,Set<String> permissions,String correlationId) {
    void require(String permission){if(!permissions.contains(permission))throw ApiException.forbidden("PERMISSION_DENIED","Missing permission: "+permission);}
    boolean staff(){return employeeId!=null;}
}

@Component
class ActorResolver {
    Actor resolve(HttpServletRequest r){
        String user=header(r,"X-User-Id");
        Set<String> p=Optional.ofNullable(r.getHeader("X-Permissions")).stream().flatMap(v->Arrays.stream(v.split(","))).map(String::trim).filter(v->!v.isBlank()).collect(Collectors.toUnmodifiableSet());
        return new Actor(user,nullable(r.getHeader("X-Customer-Id")),nullable(r.getHeader("X-Employee-Id")),nullable(r.getHeader("X-Branch-Id")),p,
                Optional.ofNullable((String)r.getAttribute("correlationId")).orElseGet(()->UUID.randomUUID().toString()));
    }
    private String header(HttpServletRequest r,String n){String v=r.getHeader(n);if(v==null||v.isBlank())throw ApiException.forbidden("AUTHENTICATION_REQUIRED",n+" is required");return v;}
    private String nullable(String v){return v==null||v.isBlank()?null:v;}
}

@Component
class CorrelationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String id=Optional.ofNullable(request.getHeader("X-Correlation-Id")).filter(v->!v.isBlank()).orElseGet(()->UUID.randomUUID().toString());
        request.setAttribute("correlationId",id);response.setHeader("X-Correlation-Id",id);chain.doFilter(request,response);
    }
}
