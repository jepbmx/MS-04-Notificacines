package com.ecommerce.notificaciones.dto;

import com.ecommerce.notificaciones.model.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Integer idNotification,
        String title,
        String content,
        String type,
        NotificationStatus status,
        boolean read,
        Instant createdAt,
        Instant sentAt,
        Instant readAt
) {
}
