package com.harshul.demo.kyc.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.harshul.demo.kyc.engine.DefaultVerificationEngine;
import com.harshul.demo.kyc.engine.VerificationAction;
import com.harshul.demo.kyc.engine.VerificationEngine;
import com.harshul.demo.kyc.engine.action.DecisionAction;
import com.harshul.demo.kyc.engine.action.InputValidationAction;
import com.harshul.demo.kyc.engine.action.PdfSignatureCheckAction;
import com.harshul.demo.kyc.engine.action.TemplateMatchingAction;
import java.util.List;

@Configuration
public class VerificationEngineConfiguration {
    @Bean
    public VerificationEngine verificationEngine() {
        List<VerificationAction> actions = List.of(
                new InputValidationAction(),
                new PdfSignatureCheckAction(),
                new TemplateMatchingAction(),
                new DecisionAction()
        );

        return new DefaultVerificationEngine(actions);
    }
}