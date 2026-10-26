package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.NotificationCreateRequest;
import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.dto.NotificationStatusResponse;
import com.ecommerce.notificaciones.dto.PagedResponse;
import com.ecommerce.notificaciones.model.NotificationStatus;
import com.ecommerce.notificaciones.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SERVICE', 'ADMIN')")
    public ResponseEntity<NotificationResponse> create(
            @Valid @RequestBody NotificationCreateRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) @Size(max = 120) String idempotencyKey) {
        NotificationResponse response = notificationService.create(request, idempotencyKey);
        if (response.id() == null) {
            throw new IllegalStateException("A persisted notification must have an identifier.");
        }
        URI location = URI.create("/api/notifications/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<PagedResponse<NotificationResponse>> getMine(
            Authentication authentication,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationResponse> notifications = notificationService.getForReceiver(
                authentication.getName(), status, type, read, page, size);
        return ResponseEntity.ok(PagedResponse.from(notifications));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationResponse> notifications = notificationService.getForReceiver(
                authentication.getName(), status, type, read, page, size);
        return ResponseEntity.ok(PagedResponse.from(notifications));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<NotificationStatusResponse> getStatus(
            @PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.getStatus(id, authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(
            @PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.getById(id, authentication.getName()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.markRead(id, authentication.getName()));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponse> retry(@PathVariable Integer id) {
        return ResponseEntity.ok(notificationService.retry(id));
    }
}
