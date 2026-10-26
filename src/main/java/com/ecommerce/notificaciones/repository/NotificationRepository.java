package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.Notification;
import com.ecommerce.notificaciones.model.NotificationStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    Optional<Notification> findByIdNotificationAndIdReceiver(Integer idNotification, String idReceiver);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.idNotification = :id")
    Optional<Notification> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            select n from Notification n
            where n.idReceiver = :receiver
              and (:status is null or n.status = :status)
              and (:type is null or n.type = :type)
              and (:read is null or n.read = :read)
            """)
    Page<Notification> findForReceiver(
            @Param("receiver") String receiver,
            @Param("status") NotificationStatus status,
            @Param("type") String type,
            @Param("read") Boolean read,
            Pageable pageable);
}
