package com.harshul.demo.kyc.engine.result;
import java.util.List;

public record InputValidationResult(
        boolean valid,
        List<String> errors
) {
    public InputValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static InputValidationResult success() {
        return new InputValidationResult(true, List.of());
    }

    public static InputValidationResult failure(List<String> errors) {
        return new InputValidationResult(false, errors);
    }
}