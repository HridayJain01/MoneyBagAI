package com.moneybags.security.exception;
import java.time.Instant;
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {}
