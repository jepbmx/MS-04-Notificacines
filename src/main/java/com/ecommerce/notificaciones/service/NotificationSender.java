package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.model.Notification;

public interface NotificationSender {

    DeliveryResult send(Notification notification);
}
