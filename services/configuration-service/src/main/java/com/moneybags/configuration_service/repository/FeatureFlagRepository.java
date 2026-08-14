package com.moneybags.configuration_service.repository;


import com.moneybags.configuration_service.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
}
