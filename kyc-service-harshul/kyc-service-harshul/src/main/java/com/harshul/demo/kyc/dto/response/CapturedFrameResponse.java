package com.harshul.demo.kyc.dto.response;

public record CapturedFrameResponse(
        String frameId,
        String sessionId,
        int frameNumber,
        String fileName,
        String contentType,
        int size
) {}
