package com.moneybags.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.account.api.ApiModels.CreateApplicationRequest;
import com.moneybags.account.api.ApiModels.DecisionRequest;
import com.moneybags.account.api.InternalModels.AccountEvent;
import com.moneybags.account.client.CustomerClient;
import com.moneybags.account.client.ProductClient;
import com.moneybags.account.client.TransactionClient.OpeningDepositCommand;
import com.moneybags.account.entity.Account;
import com.moneybags.account.entity.AccountOutbox;
import com.moneybags.account.repository.AccountOutboxRepository;
import com.moneybags.account.repository.AccountRepository;
import com.moneybags.account.security.RequestActor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({AccountApplicationService.class, AccountEventPublisher.class,
        AccountOpeningIntegrationTest.JacksonConfiguration.class})
class AccountOpeningIntegrationTest {

    @Autowired AccountApplicationService applications;
    @Autowired AccountRepository accounts;
    @Autowired AccountOutboxRepository outbox;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductClient productClient;
    @MockBean CustomerClient customerClient;

    @Test
    void approvalProjectsANonNullTimestampAndQueuesTheRequestedOpeningDeposit() throws Exception {
        when(customerClient.eligibility("CIF-OPEN-1"))
                .thenReturn(new CustomerClient.EligibilityResponse(true, "VERIFIED", "ACTIVE", null));
        when(productClient.effective("SAV-REG")).thenReturn(new ProductClient.EffectiveProduct(
                "SAV-REG", "Regular Savings", "SAVINGS", "INR",
                new BigDecimal("3.5"), new BigDecimal("1000"), new BigDecimal("1000"),
                new BigDecimal("50000"), 5, null, BigDecimal.ZERO,
                false, false, 18, "ACTIVE", "2026-08-16"));

        RequestActor maker = new RequestActor("EMP-1", "BR001",
                Set.of(RequestActor.PERMISSION_OPEN), "create-correlation");
        RequestActor checker = new RequestActor("EMP-2", "BR001",
                Set.of(RequestActor.PERMISSION_APPROVE), "approve-correlation");

        var application = applications.create(maker, new CreateApplicationRequest(
                "CIF-OPEN-1", "SAV-REG", "Opening Deposit Test",
                new BigDecimal("5000"), "INR"));
        var approved = applications.approve(checker, application.applicationId(),
                new DecisionRequest("approved"));

        Account account = accounts.findById(approved.createdAccountId()).orElseThrow();
        assertThat(account.getUpdatedAt()).isNotNull();

        List<AccountOutbox> events = outbox.findAll();
        assertThat(events).hasSize(3);

        AccountOutbox statementEvent = events.stream()
                .filter(e -> AccountEventPublisher.DESTINATION_STATEMENT.equals(e.getDestination()))
                .findFirst().orElseThrow();
        AccountEvent projectedAccount = objectMapper.readValue(
                statementEvent.getPayload(), AccountEvent.class);
        assertThat(projectedAccount.sourceUpdatedAt()).isNotNull();
        assertThat(statementEvent.getEventId()).isEqualTo(projectedAccount.sourceEventId());

        AccountOutbox transactionEvent = events.stream()
                .filter(e -> AccountEventPublisher.DESTINATION_TRANSACTION.equals(e.getDestination()))
                .findFirst().orElseThrow();
        OpeningDepositCommand command = objectMapper.readValue(
                transactionEvent.getPayload(), OpeningDepositCommand.class);
        assertThat(command.accountId()).isEqualTo(account.getAccountId());
        assertThat(command.amount()).isEqualByComparingTo("5000");
        assertThat(command.applicationReference()).isEqualTo(application.applicationReference());
        assertThat(command.initiatedByEmployeeId()).isEqualTo("EMP-2");
        assertThat(command.correlationId()).isEqualTo("approve-correlation");
    }

    @TestConfiguration
    static class JacksonConfiguration {
        @Bean ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
