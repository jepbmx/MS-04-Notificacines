
package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.CreateNotificationRequest;
import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.dto.NotificationStatusResponse;
import com.ecommerce.notificaciones.model.NotificationStatus;
import com.ecommerce.notificaciones.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.notificaciones.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Set;

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

        String idReceiver = currentUserId(jwt);

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

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> searchMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String[] sort) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100"
            );
        }

        String idReceiver = jwt.getClaim("userId").toString();
        Pageable pageable = PageRequest.of(
                page,
                size,
                notificationSort(sort)
        );
        return ResponseEntity.ok(
                notificationService.searchMyNotifications(
                        idReceiver, type, status, read, pageable
                )
        );
    }

    private Sort notificationSort(String[] sort) {
        if (sort == null || sort.length == 0) {
            return Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("idNotification")
            );
        }

        Set<String> allowedProperties = Set.of(
                "createdAt", "idNotification", "type", "status", "read"
        );
        ArrayList<Sort.Order> orders = new ArrayList<>();
        for (String sortParameter : sort) {
            String[] parts = sortParameter.split(",", -1);
            if (parts.length > 2 || parts[0].isBlank()
                    || !allowedProperties.contains(parts[0])) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "sort contiene un campo no permitido"
                );
            }

            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length == 2) {
                try {
                    direction = Sort.Direction.fromString(parts[1]);
                } catch (IllegalArgumentException ex) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "La dirección de sort debe ser asc o desc"
                    );
                }
            }
            orders.add(new Sort.Order(direction, parts[0]));
        }

        if (orders.stream().noneMatch(
                order -> order.getProperty().equals("idNotification")
        )) {
            orders.add(Sort.Order.desc("idNotification"));
        }
        return Sort.by(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(
            @PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt) {
        if (id == null || id < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser positivo"
            );
        }

        return ResponseEntity.ok(
                notificationService.getNotificationById(
                        id,
                        jwt.getClaim("userId").toString()
                )
        );
    }

    // PATCH /api/notifications/{id}/read
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable("id") Integer idNotification,
            @AuthenticationPrincipal Jwt jwt) {

        String idReceiver = currentUserId(jwt);

        return ResponseEntity.ok(
                notificationService.markNotificationAsRead(
                        idNotification,
                        idReceiver
                )
        );
    }

    // GET /api/notifications/{id}/status
    @GetMapping("/{id}/status")
    public ResponseEntity<NotificationStatusResponse> getStatus(
            @PathVariable("id") Integer idNotification,
            @AuthenticationPrincipal Jwt jwt) {

        String idReceiver = currentUserId(jwt);

        return ResponseEntity.ok(
                notificationService.getNotificationStatus(idNotification, idReceiver)
        );
    }

    private String currentUserId(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        if (userId == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_TOKEN",
                    "El token no contiene el identificador del usuario"
            );
        }
        return userId.toString();
    }


}
