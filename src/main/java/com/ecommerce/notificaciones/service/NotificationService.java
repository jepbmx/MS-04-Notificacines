package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.dto.NotificationCreateRequest;
import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.dto.NotificationStatusResponse;
import com.ecommerce.notificaciones.exception.NotificationConflictException;
import com.ecommerce.notificaciones.exception.NotificationNotFoundException;
import com.ecommerce.notificaciones.model.Notification;
import com.ecommerce.notificaciones.model.NotificationAttempt;
import com.ecommerce.notificaciones.model.AttemptStatus;
import com.ecommerce.notificaciones.model.NotificationStatus;
import com.ecommerce.notificaciones.repository.NotificationAttemptRepository;
import com.ecommerce.notificaciones.repository.NotificationRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository attemptRepository;
    private final NotificationSender notificationSender;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationAttemptRepository attemptRepository,
            NotificationSender notificationSender) {
        this.notificationRepository = notificationRepository;
        this.attemptRepository = attemptRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public NotificationResponse create(NotificationCreateRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedKey != null) {
            var existing = notificationRepository.findByIdempotencyKey(normalizedKey);
            if (existing.isPresent()) {
                return NotificationResponse.from(existing.get());
            }
        }

        Notification notification = new Notification(
                request.idReceiver().trim(),
                request.title().trim(),
                request.content().trim(),
                request.type().trim());
        notification.setIdempotencyKey(normalizedKey);
        notification = notificationRepository.save(notification);
        processDelivery(notification, 1);
        return NotificationResponse.from(notification);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Integer id, String receiver) {
        return NotificationResponse.from(findOwned(id, receiver));
    }

    @Transactional(readOnly = true)
    public NotificationStatusResponse getStatus(Integer id, String receiver) {
        return NotificationStatusResponse.from(findOwned(id, receiver));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getForReceiver(
            String receiver,
            NotificationStatus status,
            String type,
            Boolean read,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findForReceiver(receiver, status, normalizeFilter(type), read, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markRead(Integer id, String receiver) {
        Notification notification = findOwned(id, receiver);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse retry(Integer id) {
        Notification notification = notificationRepository.findByIdForUpdate(id)
                .orElseThrow(NotificationNotFoundException::new);
        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new NotificationConflictException("Only failed notifications can be retried.");
        }
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            throw new NotificationConflictException("The maximum number of retries has been reached.");
        }

        notification.setRetryCount(notification.getRetryCount() + 1);
        processDelivery(notification, notification.getRetryCount() + 1);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    private void processDelivery(Notification notification, int attemptNumber) {
        notification.setStatus(NotificationStatus.PROCESSING);
        DeliveryResult result = notificationSender.send(notification);
        if (result.successful()) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setLastErrorCode(null);
            attemptRepository.save(new NotificationAttempt(
                    notification, attemptNumber, AttemptStatus.SUCCESS, null, null));
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setSentAt(null);
            notification.setLastErrorCode(result.errorCode());
            attemptRepository.save(new NotificationAttempt(
                    notification, attemptNumber, AttemptStatus.FAILED,
                    result.errorCode(), result.errorMessage()));
        }
    }

    private Notification findOwned(Integer id, String receiver) {
        return notificationRepository.findByIdNotificationAndIdReceiver(id, receiver)
                .orElseThrow(NotificationNotFoundException::new);
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalizedKey = key.trim();
        if (normalizedKey.length() > 120) {
            throw new IllegalArgumentException("Idempotency key must not exceed 120 characters.");
        }
        return normalizedKey;
    }

    private String normalizeFilter(String filter) {
        return filter == null || filter.isBlank() ? null : filter.trim();
    }
}
