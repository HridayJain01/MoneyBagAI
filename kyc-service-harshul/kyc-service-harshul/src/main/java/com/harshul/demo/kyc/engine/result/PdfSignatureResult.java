package com.harshul.demo.kyc.engine.result;
import java.time.Instant;
import java.util.List;

public record PdfSignatureResult(
        boolean signaturePresent,
        boolean signatureValid,
        List<SignerInfo> signers,
        List<String> errors
) {
    public PdfSignatureResult {
        signers = signers == null ? List.of() : List.copyOf(signers);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static PdfSignatureResult failure(
            boolean signaturePresent,
            String error
    ) {
        return new PdfSignatureResult(
                signaturePresent,
                false,
                List.of(),
                List.of(error)
        );
    }

    public record SignerInfo(
            String subject,
            String issuer,
            String serialNumber,
            Instant validFrom,
            Instant validUntil
    ) {}
}