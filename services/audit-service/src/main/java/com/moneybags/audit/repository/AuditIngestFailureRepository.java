package com.moneybags.audit.repository;

import com.moneybags.audit.entity.AuditIngestFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditIngestFailureRepository extends JpaRepository<AuditIngestFailure, Long> {
}
