package com.ecommerce.notificaciones.dto;

import com.ecommerce.notificaciones.model.NotificationStatus;
import com.ecommerce.notificaciones.model.Notifications;

import java.time.Instant;

// Salida: no se expone la entidad directamente.
// status = estado de ENTREGA; read = estado de LECTURA (son conceptos distintos).
public record NotificationResponse(
        Integer id,
        String idReceiver,
        String type,
        String title,
        String content,
        NotificationStatus status,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notifications n) {
        return new NotificationResponse(n.getIdNotification(), n.getIdReceiver(), n.getType(),
                n.getTitle(), n.getContent(), n.getStatus(), n.isRead(), n.getCreatedAt());
    }
}