package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Optional<NotificationResponse> findByIdForReceiver(Integer id, String receiverId) {
        return notificationRepository.findByIdNotificationAndIdReceiver(id, receiverId)
                .map(NotificationResponse::from);
    }
}
