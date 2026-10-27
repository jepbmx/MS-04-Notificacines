package com.ecommerce.notificaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_processed_event_notification",
                columnNames = "id_notification"
        )
)
public class ProcessedEvents {

    @Id
    @NotBlank
    @Size(max = 120)
    @Column(name = "event_id", nullable = false, length = 120)
    private String eventId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_notification", nullable = false, unique = true)
    private Notification notification;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    public ProcessedEvents() {
    }

    public ProcessedEvents(String eventId, String eventType, Notification notification) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.notification = notification;
    }

    @PrePersist
    public void prePersist() {
        if (this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
