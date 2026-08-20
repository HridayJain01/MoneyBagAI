package com.harshul.demo.kyc.engine;
import java.util.List;


public record VerificationResult(
        boolean verified,
        String decision,
        double documentScore,
        double faceMatchScore,
        double livenessScore,
        double geoScore,
        double finalScore,
        List<String> errors
) {
    public VerificationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}