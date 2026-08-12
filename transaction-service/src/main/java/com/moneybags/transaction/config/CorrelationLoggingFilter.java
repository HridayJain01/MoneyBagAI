package com.moneybags.transaction.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationLoggingFilter extends org.springframework.web.filter.OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String correlation=request.getHeader("X-Correlation-Id");if(correlation==null||correlation.isBlank())correlation=UUID.randomUUID().toString();
        request.setAttribute("correlationId",correlation);MDC.put("correlationId",correlation);response.setHeader("X-Correlation-Id",correlation);try{chain.doFilter(request,response);}finally{MDC.clear();}
    }
}
