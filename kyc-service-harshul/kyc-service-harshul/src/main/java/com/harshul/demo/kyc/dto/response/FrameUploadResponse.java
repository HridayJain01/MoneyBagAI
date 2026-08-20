package com.harshul.demo.kyc.dto.response;

public record FrameUploadResponse(
        String sesssionId,
        int frameCount,
        String status
){
}
