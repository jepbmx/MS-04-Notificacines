package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Optional<Notification> findByIdNotificationAndIdReceiver(
            Integer idNotification,
            String idReceiver
    );

    Page<Notification> findByIdReceiver(String idReceiver, Pageable pageable);
}
