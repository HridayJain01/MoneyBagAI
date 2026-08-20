package com.harshul.demo.kyc.repository;

import com.harshul.demo.kyc.entity.KycSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import com.harshul.demo.kyc.entity.KycSessionStatus;


@Repository
public interface KycSessionRepository extends JpaRepository<KycSessionEntity, String> {
    Optional<KycSessionEntity> findByExternalUserId(String externalUserId);
    List<KycSessionEntity> findByExternalUserIdAndStatusInOrderByCreatedAtDesc(String externalUserId, List<KycSessionStatus> statuses);
    List<KycSessionEntity> findByExternalUserIdOrderByCreatedAtDesc(String externalUserId);
    List<KycSessionEntity> findByExternalUserIdAndDocumentTypeOrderByCreatedAtDesc(String externalUserId, com.harshul.demo.kyc.entity.DocumentType documentType);
}
