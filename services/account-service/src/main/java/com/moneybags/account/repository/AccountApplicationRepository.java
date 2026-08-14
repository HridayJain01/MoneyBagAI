package com.moneybags.account.repository;

import com.moneybags.account.entity.AccountApplication;
import com.moneybags.account.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountApplicationRepository extends JpaRepository<AccountApplication, String> {

    Optional<AccountApplication> findByApplicationReference(String applicationReference);

    @Query("""
            SELECT a FROM AccountApplication a
            WHERE (:cifNo IS NULL OR a.cifNo = :cifNo)
              AND (:branchCode IS NULL OR a.branchCode = :branchCode)
              AND (:status IS NULL OR a.status = :status)
            """)
    Page<AccountApplication> search(@Param("cifNo") String cifNo,
                                    @Param("branchCode") String branchCode,
                                    @Param("status") ApplicationStatus status,
                                    Pageable pageable);
}
