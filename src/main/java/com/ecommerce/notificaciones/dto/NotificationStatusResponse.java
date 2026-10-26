package com.ecommerce.notificaciones.dto;

import com.ecommerce.notificaciones.model.Notification;
import com.ecommerce.notificaciones.model.NotificationStatus;
import java.time.LocalDateTime;

public record NotificationStatusResponse(
        Integer id,
        NotificationStatus deliveryStatus,
        boolean read,
        int retryCount,
        int maxRetries,
        String lastErrorCode,
        LocalDateTime sentAt) {

    public static NotificationStatusResponse from(Notification notification) {
        return new NotificationStatusResponse(
                notification.getIdNotification(),
                notification.getStatus(),
                notification.isRead(),
                notification.getRetryCount(),
                notification.getMaxRetries(),
                notification.getLastErrorCode(),
                notification.getSentAt());
    }
}
