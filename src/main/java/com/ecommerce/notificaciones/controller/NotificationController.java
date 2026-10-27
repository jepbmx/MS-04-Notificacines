
package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.CreateNotificationRequest;
import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }


    // POST /api/notifications
    @PostMapping
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey) {

        NotificationService.CreationResult result =
                notificationService.create(request, idempotencyKey);

        if (!result.created()) {
            return ResponseEntity.ok(result.notification());
        }

        URI location = URI.create(
                "/api/notifications/" + result.notification().id()
        );

        return ResponseEntity.created(location)
                .body(result.notification());
    }

    // GET /api/notifications/me
    @GetMapping("/me")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Number userId = jwt.getClaim("userId");
        assert userId != null;
        String idReceiver = userId.toString();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("idNotification")
                )
        );

        return ResponseEntity.ok(
                notificationService.getMyNotifications(idReceiver, pageable)
        );
    }

    // PATCH /api/notifications/{id}/read
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable("id") Integer idNotification,
            @AuthenticationPrincipal Jwt jwt) {

        Number userId = jwt.getClaim("userId");
        assert userId != null;
        String idReceiver = userId.toString();

        return ResponseEntity.ok(
                notificationService.markNotificationAsRead(
                        idNotification,
                        idReceiver
                )
        );
    }


}
