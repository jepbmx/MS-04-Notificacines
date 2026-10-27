package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.CreateNotificationRequest;
import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    // POST /api/notifications -> 201 + Location si se creó; 200 si la Idempotency-Key ya existía
    @PostMapping
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        NotificationService.CreationResult result = service.create(request, idempotencyKey);
        if (!result.created()) {
            return ResponseEntity.ok(result.notification());
        }
        URI location = URI.create("/api/notifications/" + result.notification().id());
        return ResponseEntity.created(location).body(result.notification());
    }
}