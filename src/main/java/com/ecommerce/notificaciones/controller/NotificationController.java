
package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.CreateNotificationRequest;
import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.dto.NotificationStatusResponse;
import com.ecommerce.notificaciones.model.NotificationStatus;
import com.ecommerce.notificaciones.service.NotificationService;
import org.springframework.web.bind.WebDataBinder;
import java.beans.PropertyEditorSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Set;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones", description = "Registro, consulta propia, lectura y reintentos de entrega simulada")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @InitBinder("sort")
    public void preserveSortPairs(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new PropertyEditorSupport() {
            @Override public void setAsText(String text) { setValue(new String[]{text}); }
        });
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "page debe ser >= 0 y size debe estar entre 1 y 100");
    }


    // POST /api/notifications
    @PostMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Notificacion creada; Location contiene su consulta"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repeticion idempotente; no agrega intentos")
    })
    @Operation(summary = "Registrar y simular entrega", description = "SERVICE o ADMINISTRADOR. Idempotency-Key repite sin crear ni enviar de nuevo. 201 nuevo; 200 repetido; 409 conflicto.")
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
    @Operation(summary = "Listar notificaciones propias", description = "Paginacion page >= 0, size 1..100; orden por fecha e ID descendentes.")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        validatePage(page, size);
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
    @Operation(summary = "Buscar notificaciones propias", description = "Filtros opcionales type, status y read. sort=createdAt,desc; repetir sort para varios criterios. size 1..100.")
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

        String idReceiver = currentUserId(jwt);
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
    @Operation(summary = "Consultar una notificacion propia", description = "404 si no existe o pertenece a otro usuario.")
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
                        currentUserId(jwt)
                )
        );
    }

    // PATCH /api/notifications/{id}/read
    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar como leida", description = "Idempotente: conserva readAt al repetir. No modifica el estado de entrega.")
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
    @Operation(summary = "Consultar estado de entrega", description = "Solo ADMINISTRADOR puede consultar el estado de cualquier notificación. finalState y willRetry indican estado terminal y elegibilidad para reintento manual.")
    public ResponseEntity<NotificationStatusResponse> getStatus(
            @PathVariable("id") Integer idNotification) {

        if (idNotification == null || idNotification < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser positivo"
            );
        }

        return ResponseEntity.ok(
                notificationService.getNotificationStatusForAdmin(idNotification)
        );
    }

    // POST /api/notifications/{id}/retry
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reintento procesado y registrado en el historial de intentos")
    @Operation(summary = "Reintentar notificacion", description = "Solo ADMINISTRADOR. Ejecuta el intento y devuelve el estado actualizado de la notificacion.")
    @PostMapping("/{id}/retry")
    public ResponseEntity<NotificationResponse> retry(
            @PathVariable("id") Integer idNotification,
            @AuthenticationPrincipal Jwt jwt) {
        if (idNotification == null || idNotification < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser positivo"
            );
        }

        return ResponseEntity.ok(
                notificationService.retryNotification(idNotification)
        );
    }

    private String currentUserId(Jwt jwt) {
        Object claim = jwt == null ? null : jwt.getClaim("userId");
        if (!(claim instanceof Number userId) || userId.longValue() <= 0) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_TOKEN",
                    "El token no contiene el identificador del usuario"
            );
        }
        return Long.toString(((Number) claim).longValue());
    }


}
