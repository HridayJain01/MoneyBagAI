package com.harshul.demo.kyc.engine;

public interface VerificationAction {
    VerificationStatus execute(VerificationContext context);
}
