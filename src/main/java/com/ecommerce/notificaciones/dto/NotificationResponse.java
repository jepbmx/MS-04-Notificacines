package com.ecommerce.notificaciones.dto;

import com.ecommerce.notificaciones.model.Notification;
import com.ecommerce.notificaciones.model.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Integer id,
        String title,
        String content,
        String type,
        NotificationStatus status,
        boolean read,
        Instant readAt,
        Instant createdAt,
        Instant sentAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getIdNotification(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getStatus(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
