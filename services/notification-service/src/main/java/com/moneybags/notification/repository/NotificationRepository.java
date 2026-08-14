package com.moneybags.notification.repository;

import com.moneybags.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Optional<Notification> findByDedupKey(String dedupKey);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.nextAttemptAt <= :now "
            + "ORDER BY n.createdAt ASC")
    List<Notification> findDeliverable(@Param("status") String status,
                                       @Param("now") Instant now,
                                       Pageable pageable);

    @Query("""
            SELECT n FROM Notification n
            WHERE (:status IS NULL OR n.status = :status)
              AND (:cifNo IS NULL OR n.cifNo = :cifNo)
              AND (:recipient IS NULL OR n.recipient = :recipient)
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> search(@Param("status") String status,
                              @Param("cifNo") String cifNo,
                              @Param("recipient") String recipient,
                              Pageable pageable);
}
