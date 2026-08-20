package com.harshul.demo.kyc.entity;

public enum KycSessionStatus {
    CREATED,
    DOCUMENT_PENDING,
    DOCUMENT_UPLOADED,
    FRAME_CAPTURE_PENDING,
    FRAME_CAPTURED,
    VERIFICATION_IN_PROGRESS,
    VERIFIED,
    REJECTED,
    FAILED
}