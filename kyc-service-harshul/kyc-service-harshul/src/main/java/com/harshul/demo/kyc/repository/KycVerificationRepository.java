package com.harshul.demo.kyc.repository;


import com.harshul.demo.kyc.entity.KycVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KycVerificationRepository extends JpaRepository<KycVerificationEntity, String> {

    Optional<KycVerificationEntity> findBySessionId(String sessionId);
}