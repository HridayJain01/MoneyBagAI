package com.moneybags.customer.repository;
import com.moneybags.customer.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByCustomerCifNo(String cifNo);
    boolean existsByCustomerCifNoAndBeneficiaryAccountNoAndBeneficiaryIfsc(String cifNo, String accountNo, String ifsc);
}
