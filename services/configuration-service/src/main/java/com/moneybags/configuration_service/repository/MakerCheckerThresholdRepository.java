package com.moneybags.configuration_service.repository;

import com.moneybags.configuration_service.entity.MakerCheckerThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MakerCheckerThresholdRepository extends JpaRepository<MakerCheckerThreshold, Long> {
    List<MakerCheckerThreshold> findAll();
    List<MakerCheckerThreshold> findByActionType(String actionType);
}
