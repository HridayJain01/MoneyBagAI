package com.harshul.demo.kyc.engine.action;

import com.harshul.demo.kyc.engine.VerificationAction;
import com.harshul.demo.kyc.engine.VerificationContext;
import com.harshul.demo.kyc.engine.VerificationStatus;
import com.harshul.demo.kyc.engine.result.TemplateMatchResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

public final class TemplateMatchingAction implements VerificationAction {

    private static final double MATCH_THRESHOLD = 0.80;

    private static final double[] SCALES = {
            0.15, 0.20, 0.25, 0.30, 0.35,
            0.40, 0.45, 0.50, 0.60, 0.70
    };

    private static final int STRIDE = 10;

    @Override
    public VerificationStatus execute(VerificationContext context) {
        try {
            BufferedImage template = renderPdfFirstPage(context.request().pdfPath());

            double bestScore = bestMatchScore(
                    template,
                    context.request().framePaths()
            );

            context.put(new TemplateMatchResult(
                    bestScore >= MATCH_THRESHOLD,
                    bestScore,
                    List.of()
            ));

            return VerificationStatus.CONTINUE;

        } catch (Exception ex) {
            context.put(new TemplateMatchResult(
                    false,
                    0.0,
                    List.of("Template matching failed: " + ex.getMessage())
            ));

            return VerificationStatus.STOP;
        }
    }

    private BufferedImage renderPdfFirstPage(Path pdfPath) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (document.getNumberOfPages() == 0) {
                throw new IllegalArgumentException("PDF has no pages.");
            }

            PDFRenderer renderer = new PDFRenderer(document);

            return renderer.renderImageWithDPI(0, 120);
        }
    }

    private double bestMatchScore(
            BufferedImage template,
            List<Path> framePaths
    ) throws Exception {
        double bestScore = 0.0;

        for (Path framePath : framePaths) {
            BufferedImage frame = ImageIO.read(framePath.toFile());

            if (frame == null) {
                continue;
            }

            bestScore = Math.max(
                    bestScore,
                    bestMultiScaleScore(frame, template)
            );
        }

        return bestScore;
    }

    private double bestMultiScaleScore(
            BufferedImage frame,
            BufferedImage template
    ) {
        BufferedImage grayFrame = toGray(frame);

        double best = 0.0;

        for (double scale : SCALES) {
            BufferedImage scaledTemplate = resize(template, scale);
            BufferedImage grayTemplate = toGray(scaledTemplate);

            if (grayTemplate.getWidth() > grayFrame.getWidth()
                    || grayTemplate.getHeight() > grayFrame.getHeight()) {
                continue;
            }

            best = Math.max(
                    best,
                    slidingNormalizedDotProduct(grayFrame, grayTemplate)
            );
        }

        return best;
    }

    private double slidingNormalizedDotProduct(
            BufferedImage frame,
            BufferedImage template
    ) {
        double best = 0.0;

        int maxX = frame.getWidth() - template.getWidth();
        int maxY = frame.getHeight() - template.getHeight();

        for (int y = 0; y <= maxY; y += STRIDE) {
            for (int x = 0; x <= maxX; x += STRIDE) {
                best = Math.max(
                        best,
                        normalizedDotProductAt(frame, template, x, y)
                );
            }
        }

        return best;
    }

    private double normalizedDotProductAt(
            BufferedImage frame,
            BufferedImage template,
            int offsetX,
            int offsetY
    ) {
        double dot = 0.0;
        double frameNorm = 0.0;
        double templateNorm = 0.0;

        for (int y = 0; y < template.getHeight(); y++) {
            for (int x = 0; x < template.getWidth(); x++) {
                int framePixel = frame.getRaster()
                        .getSample(offsetX + x, offsetY + y, 0);

                int templatePixel = template.getRaster()
                        .getSample(x, y, 0);

                dot += framePixel * templatePixel;
                frameNorm += framePixel * framePixel;
                templateNorm += templatePixel * templatePixel;
            }
        }

        if (frameNorm == 0 || templateNorm == 0) {
            return 0.0;
        }

        return dot / (Math.sqrt(frameNorm) * Math.sqrt(templateNorm));
    }

    private BufferedImage toGray(BufferedImage image) {
        BufferedImage gray = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D graphics = gray.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        return gray;
    }

    private BufferedImage resize(
            BufferedImage image,
            double scale
    ) {
        int width = Math.max(1, (int) (image.getWidth() * scale));
        int height = Math.max(1, (int) (image.getHeight() * scale));

        BufferedImage resized = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D graphics = resized.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();

        return resized;
    }
}