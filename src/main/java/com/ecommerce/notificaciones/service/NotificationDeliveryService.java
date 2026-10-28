package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.model.*;
import com.ecommerce.notificaciones.repository.NotificationAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);
    private final NotificationAttemptRepository attempts;
    private final SimulatedNotificationSender sender;

    public NotificationDeliveryService(NotificationAttemptRepository attempts, SimulatedNotificationSender sender) {
        this.attempts = attempts;
        this.sender = sender;
    }

    // Participa en la transaccion de creacion; fallo de entrega se guarda, no se lanza.
    @Transactional
    public void deliver(Notifications notification) {
        notification.setStatus(NotificationStatus.PROCESSING);
        boolean sent = sender.send(notification);
        notification.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.FAILED);
        notification.setSentAt(sent ? Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS) : null);
        notification.setLastErrorCode(sent ? null : "DELIVERY_TIMEOUT");
        attempts.save(new NotificationAttempts(notification, notification.getRetryCount() + 1,
                sent ? AttemptStatus.SUCCESS : AttemptStatus.FAILED,
                sent ? null : "DELIVERY_TIMEOUT",
                sent ? null : "El simulador rechazo la entrega"));
        if (!sent) log.warn("Entrega simulada fallida: notificationId={}, attempt={}",
                notification.getIdNotification(), notification.getRetryCount() + 1);
    }

}
