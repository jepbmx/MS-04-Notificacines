package com.ecommerce.notificaciones.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(
        name = "notification_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_attempt_number",
                columnNames = {"id_notification", "attempt_number"}
        )
)
public class NotificationAttempts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_attempt")
    private Integer idAttempt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_notification", nullable = false)
    private Notifications notification;

    @Positive
    @NotNull
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AttemptStatus status;

    @Size(max = 50)
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Size(max = 255)
    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    @PrePersist
    public void prePersist() {
        if (this.attemptedAt == null) {
            this.attemptedAt = Instant.now();
        }
    }

    public NotificationAttempts() {
    }

    public NotificationAttempts(Notifications notification, Integer attemptNumber, AttemptStatus status,
                                String errorCode, String errorMessage) {
        this.notification = notification;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Integer getIdAttempt() {
        return idAttempt;
    }

    public void setIdAttempt(Integer idAttempt) {
        this.idAttempt = idAttempt;
    }

    public Notifications getNotification() {
        return notification;
    }

    public void setNotification(Notifications notification) {
        this.notification = notification;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }
}
