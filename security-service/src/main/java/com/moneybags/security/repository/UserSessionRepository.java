package com.moneybags.security.repository;
import com.moneybags.security.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {}
