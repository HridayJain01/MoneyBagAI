package com.moneybags.customer;

import com.moneybags.customer.enums.KycStatus;
import com.moneybags.customer.repository.BeneficiaryRepository;
import com.moneybags.customer.repository.CustomerAddressRepository;
import com.moneybags.customer.repository.CustomerRepository;
import com.moneybags.customer.repository.KycDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@ActiveProfiles("demo")
class CustomerDemoProfileIntegrationTest {
    @Autowired CustomerRepository customers;
    @Autowired CustomerAddressRepository addresses;
    @Autowired KycDocumentRepository documents;
    @Autowired BeneficiaryRepository beneficiaries;

    @Test
    void loadsTheExplicitFictionalDemoDataset() {
        assertThat(customers.findAll()).hasSize(2);
        assertThat(addresses.findAll()).hasSize(3);
        assertThat(documents.findAll()).hasSize(1);
        assertThat(beneficiaries.findAll()).hasSize(1);
        assertThat(customers.findById("CIF900101").orElseThrow().getKycStatus())
                .isEqualTo(KycStatus.VERIFIED);
        assertThat(documents.findById(3001L).orElseThrow().getDocumentNumberHash())
                .hasSize(64);
    }
}
