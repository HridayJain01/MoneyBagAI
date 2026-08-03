package com.moneybags.customer.repository;
import com.moneybags.customer.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByCustomerCifNo(String cifNo);
    List<KycDocument> findByVerifyStatus(com.moneybags.customer.enums.DocumentVerifyStatus status);
    List<KycDocument> findByExpiryDateBefore(java.time.LocalDate date);
}
