package com.moneybags.transaction.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RestControllerAdvice
public class ApiExceptionHandler {
    public record ErrorResponse(Instant timestamp,int status,String error,String code,String message,String path,String correlationId,Map<String,String> fieldErrors){}
    @ExceptionHandler(DomainException.class) ResponseEntity<ErrorResponse> domain(DomainException ex,HttpServletRequest request){return build(ex.getStatus(),ex.getCode(),ex.getMessage(),request,Map.of());}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex,HttpServletRequest request){
        Map<String,String> fields=new LinkedHashMap<>(); ex.getBindingResult().getFieldErrors().forEach(e->fields.putIfAbsent(e.getField(),e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST,"REQUEST_VALIDATION_FAILED","Request validation failed",request,fields);
    }
    @ExceptionHandler(NoResourceFoundException.class) ResponseEntity<ErrorResponse> notFound(NoResourceFoundException ex,HttpServletRequest request){return build(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","The requested resource was not found",request,Map.of());}
    @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unexpected(Exception ex,HttpServletRequest request){log.error("unhandled_transaction_service_error path={}",request.getRequestURI(),ex);return build(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","An unexpected error occurred",request,Map.of());}
    private ResponseEntity<ErrorResponse> build(HttpStatus status,String code,String message,HttpServletRequest request,Map<String,String> fields){
        String cid=Optional.ofNullable(request.getHeader("X-Correlation-Id")).filter(s->!s.isBlank()).orElse("unavailable");
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(),status.value(),status.getReasonPhrase(),code,message,request.getRequestURI(),cid,fields));
    }
}
