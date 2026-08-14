package com.moneybags.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component @RequiredArgsConstructor
public class RequestHasher {
    private final ObjectMapper mapper;
    public String hash(Object value){
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e){throw new IllegalStateException("Unable to hash request",e);}
    }
}
