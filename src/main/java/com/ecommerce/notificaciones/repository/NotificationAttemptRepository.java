package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.NotificationAttempts;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempts, Integer> {
    long countByNotificationIdNotification(Integer idNotification);
}
