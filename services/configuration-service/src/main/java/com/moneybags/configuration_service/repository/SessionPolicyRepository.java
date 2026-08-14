package com.moneybags.configuration_service.repository;

import com.moneybags.configuration_service.entity.SessionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionPolicyRepository extends JpaRepository<SessionPolicy, Long> {
    Optional<SessionPolicy> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDateTime asOf);
    List<SessionPolicy> findAllByOrderByEffectiveFromDesc();
}
