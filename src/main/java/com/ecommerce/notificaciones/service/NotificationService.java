
package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.model.Notifications;
import com.ecommerce.notificaciones.repository.NotificationRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(
            String idReceiver,
            Pageable pageable
    ) {
        if (idReceiver == null || idReceiver.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del usuario es obligatorio"
            );
        }

        return notificationRepository
                .findByIdReceiver(idReceiver, pageable)
                .map(this::toResponse);
    }

    private NotificationResponse toResponse(Notifications notification) {
        return new NotificationResponse(
                notification.getIdNotification(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getStatus(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getReadAt()
        );
    }


    @Transactional
    public NotificationResponse markNotificationAsRead(
            Integer idNotification,
            String idReceiver
    ) {
        if (idReceiver == null || idReceiver.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del usuario es obligatorio"
            );
        }

        Notifications notification = notificationRepository
                .findById(idNotification)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notificación no encontrada"
                ));

        if (!notification.getIdReceiver().equals(idReceiver)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Notificación no encontrada"
            );
        }

        notification.markAsRead();

        return toResponse(notification);
    }

}
