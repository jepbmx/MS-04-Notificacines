package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.NotificationTemplates;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplates, Integer> {
    // Plantilla activa para un tipo de notificación
    Optional<NotificationTemplates> findByTypeAndActiveTrue(String type);
}