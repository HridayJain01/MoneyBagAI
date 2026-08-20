package com.harshul.demo.kyc.engine.result;
import java.util.List;

public record DecisionResult(
        boolean verified,
        String decision,
        double finalScore,
        List<String> errors
) {
    public DecisionResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}