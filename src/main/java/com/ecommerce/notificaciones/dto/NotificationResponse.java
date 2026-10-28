
package com.ecommerce.notificaciones.dto;

import com.ecommerce.notificaciones.model.Notifications;
import com.ecommerce.notificaciones.model.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Integer id,
        String idReceiver,
        String title,
        String content,
        String type,
        NotificationStatus status,
        boolean read,
        Instant createdAt,
        Instant sentAt,
        Instant readAt
) {

    public static NotificationResponse from(Notifications n) {
        return new NotificationResponse(
                n.getIdNotification(),
                n.getIdReceiver(),
                n.getTitle(),
                n.getContent(),
                n.getType(),
                n.getStatus(),
                n.isRead(),
                n.getCreatedAt(),
                n.getSentAt(),
                n.getReadAt()
        );
    }
}
