package com.harshul.demo.kyc.engine.action;
import com.harshul.demo.kyc.engine.VerificationAction;
import com.harshul.demo.kyc.engine.VerificationContext;
import com.harshul.demo.kyc.engine.VerificationStatus;
import com.harshul.demo.kyc.engine.VerificationResult;
import com.harshul.demo.kyc.engine.result.InputValidationResult;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class InputValidationAction implements VerificationAction {

    @Override
    public VerificationStatus execute(VerificationContext context) {
        List<String> errors = new ArrayList<>();
        if (context.request().pdfPath() == null) {
            errors.add("PDF path is missing.");
        } else if (!Files.exists(context.request().pdfPath())) {
            errors.add("PDF file does not exist: " + context.request().pdfPath());
        }

        if (context.request().framePaths().isEmpty()) {
            errors.add("No frames found for verification.");
        }

        for (var frame : context.request().framePaths()) {
            if (!Files.exists(frame)) {
                errors.add("Frame file does not exist: " + frame);
            }
        }

        if (!errors.isEmpty()) {
            context.put(InputValidationResult.failure(errors));
            context.put(new VerificationResult(
                    false,
                    "FAILED",
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    errors
            ));
            return VerificationStatus.STOP;
        }

        context.put(InputValidationResult.success());
        return VerificationStatus.CONTINUE;
    }
}