package com.moneybags.notification.repository;

import com.moneybags.notification.entity.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByNotificationIdOrderByAttemptNo(String notificationId);
}
