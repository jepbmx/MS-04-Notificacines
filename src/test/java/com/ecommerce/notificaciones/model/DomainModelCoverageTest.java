package com.ecommerce.notificaciones.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelCoverageTest {

    @Test
    void notifications_exposesStateAndHandlesReadAndRetryLifecycle() {
        Notifications notification = new Notifications();
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(0, notification.getRetryCount());
        assertEquals(3, notification.getMaxRetries());
        assertFalse(notification.isRead());
        assertNull(notification.getReadAt());

        notification.setIdNotification(12);
        notification.setIdReceiver("receiver");
        notification.setTitle("title");
        notification.setContent("content");
        notification.setType("ORDER");
        notification.setStatus(NotificationStatus.FAILED);
        notification.setIdempotencyKey("key");
        notification.setRetryCount(0);
        notification.setMaxRetries(2);
        notification.setLastErrorCode("temporary");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant sentAt = Instant.parse("2026-01-02T00:00:00Z");
        notification.setCreatedAt(createdAt);
        notification.setSentAt(sentAt);

        assertEquals(12, notification.getIdNotification());
        assertEquals("receiver", notification.getIdReceiver());
        assertEquals("title", notification.getTitle());
        assertEquals("content", notification.getContent());
        assertEquals("ORDER", notification.getType());
        assertEquals("key", notification.getIdempotencyKey());
        assertEquals("temporary", notification.getLastErrorCode());
        assertEquals(createdAt, notification.getCreatedAt());
        assertEquals(sentAt, notification.getSentAt());
        assertTrue(notification.canRetry());

        notification.prepareRetry();
        assertEquals(1, notification.getRetryCount());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertFalse(notification.canRetry());
        assertThrows(IllegalStateException.class, notification::prepareRetry);

        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(2);
        assertFalse(notification.canRetry());
        assertThrows(IllegalStateException.class, notification::prepareRetry);

        Notifications constructorNotification =
                new Notifications("another", "title", "content", "TYPE");
        assertEquals("another", constructorNotification.getIdReceiver());
        assertEquals(NotificationStatus.PENDING, constructorNotification.getStatus());

        Notifications createdAtGenerated = new Notifications();
        createdAtGenerated.prePersist();
        assertNotNull(createdAtGenerated.getCreatedAt());
        createdAtGenerated.setCreatedAt(createdAt);
        createdAtGenerated.prePersist();
        assertEquals(createdAt, createdAtGenerated.getCreatedAt());

        notification.markAsRead();
        Instant readAt = notification.getReadAt();
        assertTrue(notification.isRead());
        assertNotNull(readAt);
        notification.markAsRead();
        assertEquals(readAt, notification.getReadAt());
    }

    @Test
    void notificationAttempts_exposesFieldsAndSetsAttemptTimestamp() {
        Notifications notification = new Notifications("receiver", "title", "content", "TYPE");
        NotificationAttempts attempt = new NotificationAttempts(
                notification, 1, AttemptStatus.SUCCESS, "code", "message"
        );
        attempt.setIdAttempt(3);
        Instant attemptedAt = Instant.parse("2026-01-03T00:00:00Z");
        attempt.setAttemptedAt(attemptedAt);

        assertEquals(3, attempt.getIdAttempt());
        assertSame(notification, attempt.getNotification());
        assertEquals(1, attempt.getAttemptNumber());
        assertEquals(AttemptStatus.SUCCESS, attempt.getStatus());
        assertEquals("code", attempt.getErrorCode());
        assertEquals("message", attempt.getErrorMessage());
        assertEquals(attemptedAt, attempt.getAttemptedAt());

        attempt.setNotification(null);
        attempt.setAttemptNumber(2);
        attempt.setStatus(AttemptStatus.FAILED);
        attempt.setErrorCode("retry");
        attempt.setErrorMessage("failed");
        assertNull(attempt.getNotification());
        assertEquals(2, attempt.getAttemptNumber());
        assertEquals(AttemptStatus.FAILED, attempt.getStatus());
        assertEquals("retry", attempt.getErrorCode());
        assertEquals("failed", attempt.getErrorMessage());

        NotificationAttempts persisted = new NotificationAttempts();
        persisted.prePersist();
        assertNotNull(persisted.getAttemptedAt());
        persisted.setAttemptedAt(attemptedAt);
        persisted.prePersist();
        assertEquals(attemptedAt, persisted.getAttemptedAt());
        assertEquals(AttemptStatus.SUCCESS, AttemptStatus.valueOf("SUCCESS"));
        assertEquals(AttemptStatus.FAILED, AttemptStatus.valueOf("FAILED"));
    }

    @Test
    void notificationTemplates_exposesFieldsAndUpdatesLifecycleTimestamps() {
        NotificationTemplates template =
                new NotificationTemplates("ORDER", "Hello", "Order {{id}}");
        template.setIdTemplate(4);
        template.setType("PAYMENT");
        template.setTitleTemplate("Paid");
        template.setContentTemplate("Payment complete");
        template.setActive(false);

        assertEquals(4, template.getIdTemplate());
        assertEquals("PAYMENT", template.getType());
        assertEquals("Paid", template.getTitleTemplate());
        assertEquals("Payment complete", template.getContentTemplate());
        assertFalse(template.isActive());

        template.prePersist();
        assertNotNull(template.getCreatedAt());
        assertEquals(template.getCreatedAt(), template.getUpdatedAt());
        Instant createdAt = template.getCreatedAt();
        template.preUpdate();
        assertEquals(createdAt, template.getCreatedAt());
        assertNotNull(template.getUpdatedAt());
        assertFalse(template.getUpdatedAt().isBefore(createdAt));

        NotificationTemplates defaultTemplate = new NotificationTemplates();
        assertTrue(defaultTemplate.isActive());
    }

    @Test
    void processedEvents_exposesFieldsAndSetsProcessedTimestamp() {
        Notifications notification = new Notifications("receiver", "title", "content", "TYPE");
        ProcessedEvents event = new ProcessedEvents("event-1", "ORDER", notification);
        event.setEventId("event-2");
        event.setEventType("PAYMENT");
        event.setNotification(null);

        assertEquals("event-2", event.getEventId());
        assertEquals("PAYMENT", event.getEventType());
        assertNull(event.getNotification());

        event.prePersist();
        assertNotNull(event.getProcessedAt());
        ProcessedEvents persisted = new ProcessedEvents();
        persisted.setEventId("event-3");
        persisted.setEventType("SHIPMENT");
        persisted.setNotification(notification);
        persisted.prePersist();
        Instant generatedAt = persisted.getProcessedAt();
        persisted.prePersist();
        assertEquals(generatedAt, persisted.getProcessedAt());
        assertSame(notification, persisted.getNotification());

        assertEquals("event-1", new ProcessedEvents("event-1", "ORDER", notification).getEventId());
        assertArrayEquals(
                new NotificationStatus[]{
                        NotificationStatus.PENDING,
                        NotificationStatus.PROCESSING,
                        NotificationStatus.SENT,
                        NotificationStatus.FAILED,
                        NotificationStatus.CANCELLED
                },
                NotificationStatus.values()
        );
    }
}
