package com.harshul.demo.kyc.repository;

import com.harshul.demo.kyc.entity.DocumentType;
import com.harshul.demo.kyc.entity.KycDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocumentEntity, String> {
    List<KycDocumentEntity> findBySessionId(String sessionId);
    Optional<KycDocumentEntity> findBySessionIdAndDocumentType(String sessionId, DocumentType documentType);
}
