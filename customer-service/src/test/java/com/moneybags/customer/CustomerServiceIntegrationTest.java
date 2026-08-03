package com.moneybags.customer;

import com.moneybags.customer.client.SecurityClient;
import com.moneybags.customer.dto.CustomerOperations.*;
import com.moneybags.customer.dto.CustomerRequest;
import com.moneybags.customer.entity.*;
import com.moneybags.customer.enums.*;
import com.moneybags.customer.exception.ConflictException;
import com.moneybags.customer.repository.*;
import com.moneybags.customer.service.CustomerOperationsService;
import com.moneybags.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class CustomerServiceIntegrationTest {
    @Autowired CustomerService customerService;
    @Autowired CustomerOperationsService operations;
    @Autowired CustomerRepository customers;
    @Autowired CustomerAddressRepository addresses;
    @Autowired KycDocumentRepository documents;
    @Autowired KycRejectionHistoryRepository rejectionHistory;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired BeneficiaryChangeHistoryRepository beneficiaryHistory;
    @MockBean SecurityClient securityClient;

    @BeforeEach
    void seedH2Only() {
        beneficiaryHistory.deleteAll();
        beneficiaries.deleteAll();
        rejectionHistory.deleteAll();
        documents.deleteAll();
        addresses.deleteAll();
        customers.deleteAll();
        when(securityClient.findUser(101L)).thenReturn(new SecurityClient.UserSummary(101L, "demo.customer", "ACTIVE"));
    }

    private String createCustomer() {
        return customerService.create(new CustomerRequest(101L, "Asha", "Sharma", LocalDate.of(1995, 5, 10),
                Gender.FEMALE, "9876543210", "asha@example.com", "ABCDE1234F", CustomerStatus.ACTIVE, KycStatus.PENDING)).cifNo();
    }

    @Test
    void createsCustomerWithGeneratedCifAndRejectsDuplicatePanOrUser() {
        String cif = createCustomer();
        assertThat(cif).startsWith("CIF");
        assertThat(customers.findById(cif)).isPresent();

        assertThatThrownBy(() -> customerService.create(new CustomerRequest(101L, "Another", null, LocalDate.of(1990, 1, 1),
                Gender.MALE, "9123456789", "another@example.com", "ABCDE1234F", CustomerStatus.ACTIVE, KycStatus.PENDING)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void managesProfileManagerAndMultipleCurrentAddresses() {
        String cif = createCustomer();
        operations.update(cif, new Update("Asha", "Verma", LocalDate.of(1995, 5, 10), Gender.FEMALE, "9876543210", "asha.verma@example.com"));
        operations.assignManager(cif, 77L);
        operations.addAddress(cif, new Address(AddressType.RESIDENTIAL, "10 Park Road", "Mumbai", "Maharashtra", "400001", "India", true));
        operations.addAddress(cif, new Address(AddressType.OFFICE, "22 Business Bay", "Mumbai", "Maharashtra", "400051", "India", true));

        Customer customer = customers.findById(cif).orElseThrow();
        assertThat(customer.getLastName()).isEqualTo("Verma");
        assertThat(customer.getRelationshipManagerEmpId()).isEqualTo(77L);
        assertThat(operations.addresses(cif)).hasSize(2);
        assertThat(operations.byManager(77L)).hasSize(1);
        assertThat(((Map<?, ?>) operations.completeness(cif)).get("percentage")).isEqualTo(100);

        operations.removeManager(cif);
        assertThat(customers.findById(cif).orElseThrow().getRelationshipManagerEmpId()).isNull();
    }

    @Test
    void recordsKycRejectionAsNewAttemptThenAllowsApproval() {
        String cif = createCustomer();
        KycDocument first = (KycDocument) operations.submitKyc(cif, new KycSubmit("PAN", "ABCDE1234F", null, "/kyc/pan-v1.pdf"));
        operations.assignKyc(cif, first.getDocId(), 88L);
        operations.decideKyc(cif, first.getDocId(), new KycDecision(DocumentVerifyStatus.REJECTED, 88L, "Image is unreadable"));

        Customer customer = customers.findById(cif).orElseThrow();
        assertThat(customer.getKycStatus()).isEqualTo(KycStatus.REJECTED);
        assertThat(customer.getKycFailureCount()).isEqualTo(1);
        assertThat(operations.kycHistory(cif)).hasSize(1);
        assertThat(documents.findById(first.getDocId()).orElseThrow().getVerifyStatus()).isEqualTo(DocumentVerifyStatus.REJECTED);

        KycDocument replacement = (KycDocument) operations.submitKyc(cif, new KycSubmit("PAN", "ABCDE1234F", null, "/kyc/pan-v2.pdf"));
        operations.decideKyc(cif, replacement.getDocId(), new KycDecision(DocumentVerifyStatus.VERIFIED, 88L, null));
        assertThat(customers.findById(cif).orElseThrow().getKycStatus()).isEqualTo(KycStatus.VERIFIED);
    }

    @Test
    void enforcesBeneficiaryCoolingPeriodAndKeepsChangeHistory() {
        String cif = createCustomer();
        Beneficiary beneficiary = (Beneficiary) operations.addBeneficiary(cif,
                new BeneficiaryRequest("Ravi Kumar", "123456789012", "Demo Bank", "DEMO0000123", "Ravi", "BANK_ACCOUNT"));

        assertThatThrownBy(() -> operations.activateBeneficiary(cif, beneficiary.getBeneficiaryId()))
                .isInstanceOf(ConflictException.class);
        assertThat(((Map<?, ?>) operations.beneficiaryEligibility(cif, beneficiary.getBeneficiaryId())).get("eligible"))
                .isEqualTo(false);

        beneficiary.setAddedAt(LocalDateTime.now().minusHours(25));
        beneficiaries.save(beneficiary);
        operations.activateBeneficiary(cif, beneficiary.getBeneficiaryId());
        assertThat(beneficiaries.findById(beneficiary.getBeneficiaryId()).orElseThrow().getStatus()).isEqualTo("ACTIVE");

        operations.updateBeneficiary(cif, beneficiary.getBeneficiaryId(),
                new BeneficiaryRequest("Ravi Kumar", "999999999999", "Demo Bank", "DEMO0000123", "Ravi Updated", "BANK_ACCOUNT"));
        assertThat(beneficiaries.findById(beneficiary.getBeneficiaryId()).orElseThrow().getStatus()).isEqualTo("PENDING_ACTIVATION");
        assertThat(operations.beneficiaryHistory(beneficiary.getBeneficiaryId())).hasSize(3);
    }
}
