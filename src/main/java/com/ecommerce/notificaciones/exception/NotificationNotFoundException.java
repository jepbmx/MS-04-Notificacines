package com.ecommerce.notificaciones.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException() {
        super("Notification not found.");
    }
}
