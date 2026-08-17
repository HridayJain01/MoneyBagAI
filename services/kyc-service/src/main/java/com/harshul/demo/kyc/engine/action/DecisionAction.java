package com.harshul.demo.kyc.engine.action;

import com.harshul.demo.kyc.engine.VerificationAction;
import com.harshul.demo.kyc.engine.VerificationContext;
import com.harshul.demo.kyc.engine.VerificationStatus;
import com.harshul.demo.kyc.engine.VerificationResult;
import com.harshul.demo.kyc.engine.result.DecisionResult;
import com.harshul.demo.kyc.engine.result.PdfSignatureResult;
import com.harshul.demo.kyc.engine.result.TemplateMatchResult;

import java.util.ArrayList;
import java.util.List;

public final class DecisionAction implements VerificationAction {

    private static final double FACE_MATCH_WEIGHT = 0.50;
    private static final double DOCUMENT_WEIGHT = 0.50;

    @Override
    public VerificationStatus execute(VerificationContext context) {
        PdfSignatureResult signature = context.get(PdfSignatureResult.class);
        TemplateMatchResult match = context.get(TemplateMatchResult.class);

        List<String> errors = new ArrayList<>();

        double documentScore = 0.0;
        double faceMatchScore = 0.0;

        if (signature == null) {
            errors.add("PDF signature result missing.");
        } else {
            errors.addAll(signature.errors());

            if (signature.signaturePresent() && signature.signatureValid()) {
                documentScore = 1.0;
            }
        }

        if (match == null) {
            errors.add("Template matching result missing.");
        } else {
            errors.addAll(match.errors());
            faceMatchScore = match.bestScore();
        }

        double finalScore =
                (documentScore * DOCUMENT_WEIGHT)
                        + (faceMatchScore * FACE_MATCH_WEIGHT);

        String decision;
        boolean verified;

        if (documentScore == 1.0 && faceMatchScore >= 0.80) {
            decision = "VERIFIED";
            verified = true;
        } else if (documentScore == 1.0 && faceMatchScore >= 0.65) {
            decision = "REVIEW";
            verified = false;
        } else {
            decision = "REJECTED";
            verified = false;
        }

        DecisionResult decisionResult = new DecisionResult(
                verified,
                decision,
                finalScore,
                errors
        );

        context.put(decisionResult);

        context.put(new VerificationResult(
                verified,
                decision,
                documentScore,
                faceMatchScore,
                0.0,
                0.0,
                finalScore,
                errors
        ));

        return VerificationStatus.CONTINUE;
    }
}
