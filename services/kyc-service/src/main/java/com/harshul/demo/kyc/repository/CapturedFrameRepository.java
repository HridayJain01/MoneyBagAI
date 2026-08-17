package com.harshul.demo.kyc.repository;
import com.harshul.demo.kyc.entity.CapturedFrameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CapturedFrameRepository extends JpaRepository<CapturedFrameEntity, String> {
    List<CapturedFrameEntity> findBySessionIdOrderByFrameNumberAsc(String sessionId);
    Optional<CapturedFrameEntity> findBySessionIdAndFrameNumber(String sessionId, int frameNumber);
}
