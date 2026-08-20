package com.harshul.demo.kyc.engine;

import java.util.List;

public final class DefaultVerificationEngine implements VerificationEngine {

    private final List<VerificationAction> actions;

    public DefaultVerificationEngine(List<VerificationAction> actions) {
        this.actions = List.copyOf(actions);
    }

    @Override
    public VerificationResult verify(VerificationRequest request) {
        VerificationContext context = VerificationContext.of(request);

        for (VerificationAction action : actions) {
            VerificationStatus status = action.execute(context);

            if (status == VerificationStatus.STOP) {
                break;
            }
        }
        if (context.get(VerificationResult.class) == null) {
            System.out.println("object is null in context");
        }
        return context.get(VerificationResult.class);
    }
}