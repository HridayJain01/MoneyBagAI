package com.moneybags.transaction.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RequestActorResolver {
    public RequestActor resolve(HttpServletRequest request) {
        String user=required(request,"X-User-Id");
        String correlation=Optional.ofNullable((String)request.getAttribute("correlationId")).or(()->Optional.ofNullable(request.getHeader("X-Correlation-Id"))).filter(s->!s.isBlank()).orElseGet(()->UUID.randomUUID().toString());
        Set<String> permissions=Optional.ofNullable(request.getHeader("X-Permissions")).stream().flatMap(v->Arrays.stream(v.split(",")))
                .map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toUnmodifiableSet());
        return new RequestActor(user,blankToNull(request.getHeader("X-Customer-Id")),blankToNull(request.getHeader("X-Employee-Id")),
                blankToNull(request.getHeader("X-Branch-Code")),permissions,correlation);
    }
    private String required(HttpServletRequest request,String name){ String value=request.getHeader(name); if(value==null||value.isBlank()) throw com.moneybags.transaction.exception.DomainException.forbidden("AUTHENTICATION_REQUIRED",name+" is required"); return value; }
    private String blankToNull(String value){return value==null||value.isBlank()?null:value;}
}
