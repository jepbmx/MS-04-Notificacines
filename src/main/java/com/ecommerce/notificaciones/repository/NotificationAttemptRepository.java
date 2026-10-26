package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.NotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Integer> {
}
