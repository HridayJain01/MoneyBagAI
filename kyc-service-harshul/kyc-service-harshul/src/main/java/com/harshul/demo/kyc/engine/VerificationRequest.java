package com.harshul.demo.kyc.engine;

import java.nio.file.Path;
import java.util.List;

public record VerificationRequest(
        Path pdfPath,
        List<Path> framePaths
) {
    public VerificationRequest {
        framePaths = framePaths == null ? List.of() : List.copyOf(framePaths);
    }
}
