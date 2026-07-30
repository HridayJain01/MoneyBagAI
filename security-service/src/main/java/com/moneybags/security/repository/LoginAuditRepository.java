package com.moneybags.security.repository;
import com.moneybags.security.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {}
