package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{id}")
    public NotificationResponse getById(
            @PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Number userId = jwt.getClaim("userId");
        return notificationService.findByIdForReceiver(id, userId.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
