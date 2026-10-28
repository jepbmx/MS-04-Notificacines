package com.ecommerce.notificaciones.dto;

import com.ecommerce.notificaciones.model.NotificationStatus;
import com.ecommerce.notificaciones.model.Notifications;

import java.time.Instant;

public record NotificationStatusResponse(
        Integer id,
        NotificationStatus status,
        boolean finalState,
        boolean willRetry,
        boolean read,
        Instant createdAt,
        Instant sentAt,
        Instant readAt
) {
    public static NotificationStatusResponse from(Notifications n) {
        NotificationStatus s = n.getStatus();

        boolean willRetry = n.canRetry();
        boolean finalState = s == NotificationStatus.SENT
                || s == NotificationStatus.CANCELLED
                || (s == NotificationStatus.FAILED && !willRetry);

        return new NotificationStatusResponse(
                n.getIdNotification(),
                s,
                finalState,
                willRetry,
                n.isRead(),
                n.getCreatedAt(),
                n.getSentAt(),
                n.getReadAt()
        );
    }
}