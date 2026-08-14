package com.moneybags.transaction.config;

import com.moneybags.transaction.exception.DomainException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class FeignErrorConfiguration {
    @Bean ErrorDecoder moneybagsErrorDecoder(){
        return (method,response)->map(method,response);
    }
    private Exception map(String method,Response response){
        String resource=method.contains("CardClient")?"CARD":"ACCOUNT";
        if(response.status()==404)return DomainException.notFound(resource+"_NOT_FOUND",resource+" was not found by its owning service");
        if(response.status()==409&&method.contains("reserve"))return DomainException.conflict("INSUFFICIENT_FUNDS","Account Service rejected the atomic funds hold");
        if(response.status()==409)return DomainException.conflict(resource+"_CONFLICT",resource+" validation failed in its owning service");
        if(response.status()>=400&&response.status()<500)return DomainException.invalid(resource+"_VALIDATION_FAILED",resource+" request was rejected by its owning service");
        return new DomainException(resource+"_SERVICE_UNAVAILABLE",HttpStatus.SERVICE_UNAVAILABLE,resource+" owning service is temporarily unavailable");
    }
}
