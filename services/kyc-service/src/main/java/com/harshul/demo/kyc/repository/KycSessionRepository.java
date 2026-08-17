package com.harshul.demo.kyc.repository;

import com.harshul.demo.kyc.entity.KycSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import com.harshul.demo.kyc.entity.KycSessionStatus;


@Repository
public interface KycSessionRepository extends JpaRepository<KycSessionEntity, String> {
    Optional<KycSessionEntity> findByCifNo(String cifNo);
    List<KycSessionEntity> findByCifNoAndStatusInOrderByCreatedAtDesc(String cifNo, List<KycSessionStatus> statuses);
}
