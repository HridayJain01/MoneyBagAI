package com.moneybags.configuration_service.repository;

import com.moneybags.configuration_service.entity.OtpPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpPolicyRepository extends JpaRepository<OtpPolicy, Long> {
    Optional<OtpPolicy> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDateTime asOf);
    List<OtpPolicy> findAllByOrderByEffectiveFromDesc();
}
