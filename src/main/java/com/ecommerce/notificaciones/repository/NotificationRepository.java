package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.Notifications;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notifications, Integer> {
    // Busca una notificación ya creada con esa clave de idempotencia
    Optional<Notifications> findByIdempotencyKey(String idempotencyKey);
    Optional<Notifications> findByIdNotificationAndIdReceiver(Integer idNotification, String idReceiver);
    Page<Notifications> findByIdReceiver(
            String idReceiver,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notifications n " +
            "where n.idNotification = :id and n.idReceiver = :idReceiver")
    Optional<Notifications> findByIdAndReceiverForUpdate(
            @Param("id") Integer idNotification,
            @Param("idReceiver") String idReceiver
    );
}
