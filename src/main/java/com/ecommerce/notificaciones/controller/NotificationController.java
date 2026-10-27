
package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.service.NotificationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

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
