package com.ecommerce.notificaciones.dto;

import com.ecommerce.notificaciones.model.Notification;
import com.ecommerce.notificaciones.model.NotificationStatus;
import java.time.LocalDateTime;

public record NotificationResponse(
        Integer id,
        String idReceiver,
        String title,
        String content,
        String type,
        NotificationStatus status,
        boolean read,
        LocalDateTime readAt,
        int retryCount,
        int maxRetries,
        String lastErrorCode,
        LocalDateTime createdAt,
        LocalDateTime sentAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getIdNotification(),
                notification.getIdReceiver(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType(),
                notification.getStatus(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getRetryCount(),
                notification.getMaxRetries(),
                notification.getLastErrorCode(),
                notification.getCreatedAt(),
                notification.getSentAt());
    }
}
