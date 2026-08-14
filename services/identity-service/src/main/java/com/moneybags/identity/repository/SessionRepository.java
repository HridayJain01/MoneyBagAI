package com.moneybags.identity.repository;

import com.moneybags.identity.entity.UserSession;
import com.moneybags.identity.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SessionRepository extends JpaRepository<UserSession, String> {

    List<UserSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    /**
     * Statuses are passed as parameters rather than written as JPQL enum literals --
     * literals would have to name the enum by fully-qualified type, which is brittle.
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.status = :newStatus, s.revokedAt = :at "
            + "WHERE s.userId = :userId AND s.status = :activeStatus")
    int revokeAllForUser(@Param("userId") Long userId,
                         @Param("newStatus") SessionStatus newStatus,
                         @Param("activeStatus") SessionStatus activeStatus,
                         @Param("at") Instant at);

    /**
     * Sweeps sessions whose wall-clock expiry has passed. Resolve already rejects them,
     * so this only stops the ACTIVE set growing without bound.
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.status = :expiredStatus "
            + "WHERE s.status = :activeStatus AND s.expiresAt < :now")
    int expireStale(@Param("expiredStatus") SessionStatus expiredStatus,
                    @Param("activeStatus") SessionStatus activeStatus,
                    @Param("now") Instant now);
}
