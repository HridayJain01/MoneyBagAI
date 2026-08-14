package com.moneybags.transaction.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    public DomainException(String code, HttpStatus status, String message) { super(message); this.code=code; this.status=status; }
    public static DomainException notFound(String code,String message){return new DomainException(code,HttpStatus.NOT_FOUND,message);}
    public static DomainException conflict(String code,String message){return new DomainException(code,HttpStatus.CONFLICT,message);}
    public static DomainException forbidden(String code,String message){return new DomainException(code,HttpStatus.FORBIDDEN,message);}
    public static DomainException invalid(String code,String message){return new DomainException(code,HttpStatus.BAD_REQUEST,message);}
}
