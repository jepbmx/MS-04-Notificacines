package com.ecommerce.notificaciones.repository;

import com.ecommerce.notificaciones.model.Notifications;
import com.ecommerce.notificaciones.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notifications, Integer> {
    // Busca una notificación ya creada con esa clave de idempotencia
    Optional<Notifications> findByIdempotencyKey(String idempotencyKey);
    Page<Notifications> findByIdReceiver(
            String idReceiver,
            Pageable pageable
    );

    Optional<Notifications> findByIdNotificationAndIdReceiver(
            Integer idNotification,
            String idReceiver
    );

    @Query(
            value = """
                    select n from Notifications n
                    where n.idReceiver = :idReceiver
                      and (:type is null or n.type = :type)
                      and (:status is null or n.status = :status)
                      and (:read is null or n.read = :read)
                    """,
            countQuery = """
                    select count(n) from Notifications n
                    where n.idReceiver = :idReceiver
                      and (:type is null or n.type = :type)
                      and (:status is null or n.status = :status)
                      and (:read is null or n.read = :read)
                    """
    )
    Page<Notifications> searchByReceiverAndFilters(
            @Param("idReceiver") String idReceiver,
            @Param("type") String type,
            @Param("status") NotificationStatus status,
            @Param("read") Boolean read,
            Pageable pageable
    );
}
