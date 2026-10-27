package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notifications, Integer> {
    // Busca una notificación ya creada con esa clave de idempotencia
    Optional<Notifications> findByIdempotencyKey(String idempotencyKey);
}
