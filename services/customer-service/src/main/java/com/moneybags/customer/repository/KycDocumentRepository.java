package com.moneybags.customer.repository;
import com.moneybags.customer.entity.KycDocument;
import com.moneybags.customer.enums.DocumentVerifyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByCustomerCifNo(String cifNo);
    List<KycDocument> findByVerifyStatus(DocumentVerifyStatus status);
    List<KycDocument> findByExpiryDateBefore(LocalDate date);
    List<KycDocument> findByExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate date);
    boolean existsByCustomerCifNoAndVerifyStatus(String cifNo, DocumentVerifyStatus status);

    @Query("""
            select d from KycDocument d
            where d.expiryDate is not null
              and d.expiryDate <= :throughDate
              and d.expiryAlertedAt is null
              and d.verifyStatus <> com.moneybags.customer.enums.DocumentVerifyStatus.REJECTED
            order by d.expiryDate asc
            """)
    List<KycDocument> findUnalertedExpiringThrough(@Param("throughDate") LocalDate throughDate);
}
