package com.moneybags.configuration_service.repository;

import com.moneybags.configuration_service.entity.PasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, Long> {
    Optional<PasswordPolicy> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDateTime asOf);
    List<PasswordPolicy> findAllByOrderByEffectiveFromDesc();
}
