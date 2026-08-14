package com.moneybags.identity.repository;

import com.moneybags.identity.entity.LoginAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
    Page<LoginAudit> findByUserIdOrderByEventTimeDesc(Long userId, Pageable pageable);
}
