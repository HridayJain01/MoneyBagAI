package com.harshul.demo.kyc.engine.result;

import java.util.List;

public record TemplateMatchResult(
        boolean matched,
        double bestScore,
        List<String> errors
) {
    public TemplateMatchResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}