package com.harshul.demo.kyc.engine.action;

import com.harshul.demo.kyc.engine.VerificationAction;
import com.harshul.demo.kyc.engine.VerificationContext;
import com.harshul.demo.kyc.engine.VerificationStatus;
import com.harshul.demo.kyc.engine.result.PdfSignatureResult;
import com.harshul.demo.kyc.engine.result.PdfSignatureResult.SignerInfo;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Store;

import java.io.File;
import java.io.FileInputStream;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PdfSignatureCheckAction implements VerificationAction {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public VerificationStatus execute(VerificationContext context) {
        File pdfFile = context.request().pdfPath().toFile();

        PdfSignatureResult result = verifyPdfSignature(pdfFile);
        context.put(result);

//        // Comment~
//        if (!result.signaturePresent() || !result.signatureValid()) {
//            return VerificationStatus.STOP;
//        }

        return VerificationStatus.CONTINUE;
    }

    private PdfSignatureResult verifyPdfSignature(File pdfFile) {
        List<String> errors = new ArrayList<>();
        List<SignerInfo> signers = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            List<PDSignature> signatures = document.getSignatureDictionaries();

            if (signatures.isEmpty()) {
                return PdfSignatureResult.failure(
                        false,
                        "PDF does not contain a digital signature."
                );
            }

            boolean allValid = true;

            for (PDSignature signature : signatures) {
                SingleSignatureResult single = verifySingleSignature(pdfFile, signature);

                if (!single.valid()) {
                    allValid = false;
                    errors.addAll(single.errors());
                }

                signers.addAll(single.signers());
            }

            return new PdfSignatureResult(
                    true,
                    allValid && errors.isEmpty(),
                    signers,
                    errors
            );

        } catch (Exception ex) {
            return PdfSignatureResult.failure(
                    false,
                    "PDF signature verification failed: " + ex.getMessage()
            );
        }
    }

    private SingleSignatureResult verifySingleSignature(
            File pdfFile,
            PDSignature signature
    ) {
        List<String> errors = new ArrayList<>();
        List<SignerInfo> signers = new ArrayList<>();

        try (FileInputStream inputStream = new FileInputStream(pdfFile)) {

            byte[] signedContent = signature.getSignedContent(inputStream);
            byte[] signatureContent = signature.getContents(inputStream);

            CMSSignedData signedData = new CMSSignedData(
                    new CMSProcessableByteArray(signedContent),
                    signatureContent
            );

            Store<X509CertificateHolder> certificates = signedData.getCertificates();

            Collection<SignerInformation> signerInfos =
                    signedData.getSignerInfos().getSigners();

            if (signerInfos.isEmpty()) {
                return SingleSignatureResult.failure(
                        "No signer information found in PDF signature."
                );
            }

            boolean valid = true;

            for (SignerInformation signerInfo : signerInfos) {

                Collection<X509CertificateHolder> matches =
                        certificates.getMatches(signerInfo.getSID());

                if (matches.isEmpty()) {
                    valid = false;
                    errors.add("No matching signer certificate found.");
                    continue;
                }

                X509CertificateHolder certificateHolder = matches.iterator().next();

                X509Certificate certificate =
                        new JcaX509CertificateConverter()
                                .setProvider("BC")
                                .getCertificate(certificateHolder);

                boolean signerValid =
                        signerInfo.verify(
                                new JcaSimpleSignerInfoVerifierBuilder()
                                        .setProvider("BC")
                                        .build(certificate)
                        );

                if (!signerValid) {
                    valid = false;
                    errors.add("PDF CMS signature is cryptographically invalid.");
                }

                if (System.currentTimeMillis() < certificate.getNotBefore().getTime()
                        || System.currentTimeMillis() > certificate.getNotAfter().getTime()) {
                    valid = false;
                    errors.add("Signer certificate is expired or not yet valid.");
                }

                signers.add(new SignerInfo(
                        certificate.getSubjectX500Principal().getName(),
                        certificate.getIssuerX500Principal().getName(),
                        certificate.getSerialNumber().toString(),
                        certificate.getNotBefore().toInstant(),
                        certificate.getNotAfter().toInstant()
                ));
            }

            return new SingleSignatureResult(
                    valid && errors.isEmpty(),
                    signers,
                    errors
            );

        } catch (Exception ex) {
            return SingleSignatureResult.failure(
                    "Error verifying PDF signature: " + ex.getMessage()
            );
        }
    }

    private record SingleSignatureResult(
            boolean valid,
            List<SignerInfo> signers,
            List<String> errors
    ) {
        private SingleSignatureResult {
            signers = signers == null ? List.of() : List.copyOf(signers);
            errors = errors == null ? List.of() : List.copyOf(errors);
        }

        static SingleSignatureResult failure(String error) {
            return new SingleSignatureResult(
                    false,
                    List.of(),
                    List.of(error)
            );
        }
    }
}