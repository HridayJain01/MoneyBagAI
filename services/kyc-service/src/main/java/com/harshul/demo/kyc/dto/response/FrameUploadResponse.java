package com.harshul.demo.kyc.dto.response;

public record FrameUploadResponse(
        String sessionId,
        int frameCount,
        String status
){
}
