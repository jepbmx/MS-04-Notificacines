
package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.service.NotificationService;
import com.ecommerce.notificaciones.dto.CreateNotificationRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private Jwt createJwt(Long userId) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("cliente@ejemplo.com")
                .claim("userId", userId)
                .claim("role", "CLIENTE")
                .build();
    }

    private NotificationResponse createResponse(String receiver) {
        return new NotificationResponse(
                1,
                receiver,
                "Pedido confirmado",
                "Tu pedido fue confirmado",
                "ORDER_CONFIRMED",
                null,
                false,
                Instant.parse("2026-10-27T10:00:00Z"),
                null,
                null
        );
    }

    @Test
    void getMyNotifications_debeConsultarNotificacionesDelUsuarioAutenticado() {
        Jwt jwt = createJwt(1L);

        Page<NotificationResponse> page =
                new PageImpl<>(List.of(createResponse("1")));

        when(notificationService.getMyNotifications(eq("1"), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<NotificationResponse>> response =
                notificationController.getMyNotifications(jwt, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());

        verify(notificationService)
                .getMyNotifications(eq("1"), any(Pageable.class));
    }

    @Test
    void getMyNotifications_debeDevolverPaginaVacia() {
        Jwt jwt = createJwt(1L);

        when(notificationService.getMyNotifications(eq("1"), any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseEntity<Page<NotificationResponse>> response =
                notificationController.getMyNotifications(jwt, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getMyNotifications_debeAplicarPaginacionYOrdenamiento() {
        Jwt jwt = createJwt(2L);

        when(notificationService.getMyNotifications(eq("2"), any(Pageable.class)))
                .thenReturn(Page.empty());

        notificationController.getMyNotifications(jwt, 2, 5);

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);

        verify(notificationService)
                .getMyNotifications(eq("2"), captor.capture());

        Pageable pageable = captor.getValue();

        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());

        assertEquals(
                org.springframework.data.domain.Sort.Direction.DESC,
                pageable.getSort().getOrderFor("createdAt").getDirection()
        );

        assertEquals(
                org.springframework.data.domain.Sort.Direction.DESC,
                pageable.getSort().getOrderFor("idNotification").getDirection()
        );
    }

    @Test
    void markAsRead_debeMarcarNotificacionDelUsuarioAutenticado() {
        Jwt jwt = createJwt(1L);
        NotificationResponse notification = createResponse("1");

        when(notificationService.markNotificationAsRead(1, "1"))
                .thenReturn(notification);

        ResponseEntity<NotificationResponse> response =
                notificationController.markAsRead(1, jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().id());

        verify(notificationService)
                .markNotificationAsRead(1, "1");
    }

    @Test
    void markAsRead_debeUtilizarElUserIdDelJwt() {
        Jwt jwt = createJwt(25L);

        when(notificationService.markNotificationAsRead(7, "25"))
                .thenReturn(createResponse("25"));

        notificationController.markAsRead(7, jwt);

        verify(notificationService)
                .markNotificationAsRead(7, "25");

        verify(notificationService, never())
                .markNotificationAsRead(7, "1");
    }


    @Test
    void create_debeDevolver201CuandoSeCreaNotificacion() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1",
                "ORDER_CONFIRMED",
                "Pedido confirmado",
                "Tu pedido fue confirmado",
                Map.of()
        );

        NotificationResponse notification = createResponse("1");

        when(notificationService.create(request, null))
                .thenReturn(
                        new NotificationService.CreationResult(
                                notification,
                                true
                        )
                );

        ResponseEntity<NotificationResponse> response =
                notificationController.create(request, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().id());

        assertEquals(
                "/api/notifications/1",
                response.getHeaders().getLocation().toString()
        );

        verify(notificationService).create(request, null);
    }

    @Test
    void create_debeDevolver200CuandoNotificacionYaExiste() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1",
                "ORDER_CONFIRMED",
                "Pedido confirmado",
                "Tu pedido fue confirmado",
                Map.of()
        );

        NotificationResponse notification = createResponse("1");

        when(notificationService.create(request, "evento-123"))
                .thenReturn(
                        new NotificationService.CreationResult(
                                notification,
                                false
                        )
                );

        ResponseEntity<NotificationResponse> response =
                notificationController.create(request, "evento-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().id());

        assertNull(response.getHeaders().getLocation());

        verify(notificationService)
                .create(request, "evento-123");
    }

    @Test
    void create_debeEnviarIdempotencyKeyAlServicio() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "2",
                "ORDER_CONFIRMED",
                "Pedido confirmado",
                "Contenido",
                Map.of()
        );

        NotificationResponse notification = createResponse("2");

        when(notificationService.create(request, "clave-unica-456"))
                .thenReturn(
                        new NotificationService.CreationResult(
                                notification,
                                true
                        )
                );

        notificationController.create(request, "clave-unica-456");

        verify(notificationService, times(1))
                .create(request, "clave-unica-456");
    }

}
