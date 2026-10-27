package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.Notifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository
        extends JpaRepository<Notifications, Integer> {

    Page<Notifications> findByIdReceiver(
            String idReceiver,
            Pageable pageable
    );
}
