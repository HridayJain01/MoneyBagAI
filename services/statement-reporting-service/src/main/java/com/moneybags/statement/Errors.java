package com.moneybags.statement;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

class ApiException extends RuntimeException {
    final HttpStatus status; final String code;
    ApiException(HttpStatus s,String c,String m){super(m);status=s;code=c;}
    static ApiException notFound(String c,String m){return new ApiException(HttpStatus.NOT_FOUND,c,m);}
    static ApiException forbidden(String c,String m){return new ApiException(HttpStatus.FORBIDDEN,c,m);}
    static ApiException conflict(String c,String m){return new ApiException(HttpStatus.CONFLICT,c,m);}
    static ApiException invalid(String c,String m){return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,c,m);}
    static ApiException unavailable(String c,String m){return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,c,m);}
}

@RestControllerAdvice
class ErrorHandler {
    record ErrorResponse(String code,String message,String correlationId,Instant timestamp,Map<String,String> fieldErrors) {}
    @ExceptionHandler(ApiException.class) ResponseEntity<ErrorResponse> api(ApiException e,HttpServletRequest r){return body(e.status,e.code,e.getMessage(),r,Map.of());}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e,HttpServletRequest r){Map<String,String> f=new LinkedHashMap<>();e.getBindingResult().getFieldErrors().forEach(x->f.putIfAbsent(x.getField(),x.getDefaultMessage()));return body(HttpStatus.BAD_REQUEST,"REQUEST_VALIDATION_FAILED","Request validation failed",r,f);}
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e,HttpServletRequest r){return body(HttpStatus.CONFLICT,"DATA_CONFLICT","The request conflicts with existing data",r,Map.of());}
    @ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unknown(Exception e,HttpServletRequest r){return body(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","An unexpected error occurred",r,Map.of());}
    private ResponseEntity<ErrorResponse> body(HttpStatus s,String c,String m,HttpServletRequest r,Map<String,String> f){return ResponseEntity.status(s).body(new ErrorResponse(c,m,Optional.ofNullable((String)r.getAttribute("correlationId")).orElse("unavailable"),Instant.now(),f));}
}
