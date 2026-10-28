
package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.model.Notifications;
import com.ecommerce.notificaciones.repository.NotificationRepository;
import com.ecommerce.notificaciones.repository.NotificationTemplateRepository;
import com.ecommerce.notificaciones.repository.ProcessedEventRepository;
import com.ecommerce.notificaciones.dto.CreateNotificationRequest;
import com.ecommerce.notificaciones.exception.ApiException;
import com.ecommerce.notificaciones.model.NotificationTemplates;
import com.ecommerce.notificaciones.model.ProcessedEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notifications notification;

    @BeforeEach
    void setUp() {
        notification = new Notifications(
                "1",
                "Pedido confirmado",
                "Tu pedido fue confirmado",
                "ORDER_CONFIRMED"
        );
    }

    @Test
    void getMyNotifications_debeDevolverNotificacionesDelUsuario() {
        Pageable pageable = PageRequest.of(0, 10);

        when(notificationRepository.findByIdReceiver("1", pageable))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<?> result = notificationService.getMyNotifications("1", pageable);

        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findByIdReceiver("1", pageable);
    }

    @Test
    void getMyNotifications_debeDevolverPaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);

        when(notificationRepository.findByIdReceiver("1", pageable))
                .thenReturn(Page.empty(pageable));

        Page<?> result = notificationService.getMyNotifications("1", pageable);

        assertTrue(result.isEmpty());
        verify(notificationRepository).findByIdReceiver("1", pageable);
    }

    @Test
    void getMyNotifications_debeRechazarUsuarioNulo() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.getMyNotifications(null, pageable)
        );

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void getMyNotifications_debeRechazarUsuarioVacio() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.getMyNotifications(" ", pageable)
        );

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void markNotificationAsRead_debeMarcarNotificacionPropia() {
        when(notificationRepository.findById(1))
                .thenReturn(Optional.of(notification));

        notificationService.markNotificationAsRead(1, "1");

        assertNotNull(notification.getReadAt());
        verify(notificationRepository).findById(1);
    }

    @Test
    void markNotificationAsRead_debeSerIdempotente() {
        when(notificationRepository.findById(1))
                .thenReturn(Optional.of(notification));

        notificationService.markNotificationAsRead(1, "1");
        Instant firstReadAt = notification.getReadAt();

        notificationService.markNotificationAsRead(1, "1");

        assertEquals(firstReadAt, notification.getReadAt());
        verify(notificationRepository, times(2)).findById(1);
    }

    @Test
    void markNotificationAsRead_debeRechazarNotificacionInexistente() {
        when(notificationRepository.findById(99))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> notificationService.markNotificationAsRead(99, "1")
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void markNotificationAsRead_debeRechazarNotificacionDeOtroUsuario() {
        when(notificationRepository.findById(1))
                .thenReturn(Optional.of(notification));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> notificationService.markNotificationAsRead(1, "2")
        );

        assertEquals(404, exception.getStatusCode().value());
        assertNull(notification.getReadAt());
    }

    @Test
    void markNotificationAsRead_debeRechazarUsuarioNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markNotificationAsRead(1, null)
        );

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void markNotificationAsRead_debeRechazarUsuarioVacio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markNotificationAsRead(1, " ")
        );

        verifyNoInteractions(notificationRepository);
    }


    @Test
    void create_debeCrearNotificacionSinClave() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED",
                "Pedido confirmado", "Tu pedido fue confirmado",
                null
        );

        when(notificationRepository.save(any(Notifications.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationService.CreationResult result =
                notificationService.create(request, null);

        assertTrue(result.created());
        assertEquals("1", result.notification().idReceiver());
        assertEquals("Pedido confirmado", result.notification().title());

        verify(notificationRepository).save(any(Notifications.class));
        verifyNoInteractions(processedEventRepository);
        verifyNoInteractions(templateRepository);
    }

    @Test
    void create_debeRegistrarEventoCuandoHayClave() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED",
                "Pedido confirmado", "Contenido",
                null
        );

        when(notificationRepository.findByIdempotencyKey("evento-123"))
                .thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notifications.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationService.CreationResult result =
                notificationService.create(request, "evento-123");

        assertTrue(result.created());

        ArgumentCaptor<ProcessedEvents> captor =
                ArgumentCaptor.forClass(ProcessedEvents.class);

        verify(processedEventRepository).save(captor.capture());

        assertEquals("evento-123", captor.getValue().getEventId());
        assertEquals("ORDER_CONFIRMED", captor.getValue().getEventType());
        assertEquals(
                "evento-123",
                captor.getValue().getNotification().getIdempotencyKey()
        );
    }

    @Test
    void create_debeReutilizarNotificacionConMismaClave() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED",
                "Pedido confirmado", "Contenido",
                null
        );

        Notifications existing = new Notifications(
                "1", "Pedido confirmado", "Contenido", "ORDER_CONFIRMED"
        );
        existing.setIdNotification(7);
        existing.setIdempotencyKey("evento-123");

        when(notificationRepository.findByIdempotencyKey("evento-123"))
                .thenReturn(Optional.of(existing));

        NotificationService.CreationResult result =
                notificationService.create(request, "evento-123");

        assertFalse(result.created());
        assertEquals(7, result.notification().id());

        verify(notificationRepository, never())
                .save(any(Notifications.class));
        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void create_debeRechazarClaveUsadaConOtroDestinatario() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "2", "ORDER_CONFIRMED",
                "Pedido confirmado", "Contenido",
                null
        );

        Notifications existing = new Notifications(
                "1", "Pedido confirmado", "Contenido", "ORDER_CONFIRMED"
        );

        when(notificationRepository.findByIdempotencyKey("evento-123"))
                .thenReturn(Optional.of(existing));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> notificationService.create(request, "evento-123")
        );

        assertNotNull(exception);
        verify(notificationRepository, never())
                .save(any(Notifications.class));
    }

    @Test
    void create_debeRechazarClaveDemasiadoLarga() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED",
                "Título", "Contenido",
                null
        );

        String key = "x".repeat(121);

        assertThrows(
                ApiException.class,
                () -> notificationService.create(request, key)
        );

        verifyNoInteractions(
                notificationRepository,
                templateRepository,
                processedEventRepository
        );
    }

    @Test
    void create_debeIgnorarClaveVacia() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED",
                "Título", "Contenido",
                null
        );

        when(notificationRepository.save(any(Notifications.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationService.CreationResult result =
                notificationService.create(request, "   ");

        assertTrue(result.created());

        verify(notificationRepository, never())
                .findByIdempotencyKey(any());

        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void create_debeEliminarEspaciosDeClaveYDestinatario() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                " 1 ", " ORDER_CONFIRMED ",
                "Título", "Contenido",
                null
        );

        when(notificationRepository.findByIdempotencyKey("evento-123"))
                .thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notifications.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationService.CreationResult result =
                notificationService.create(request, " evento-123 ");

        assertEquals("1", result.notification().idReceiver());
        assertEquals("ORDER_CONFIRMED", result.notification().type());

        verify(notificationRepository)
                .findByIdempotencyKey("evento-123");
    }

    @Test
    void create_debeGenerarContenidoDesdePlantilla() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED",
                null, null,
                Map.of("pedido", "ABC123")
        );

        NotificationTemplates template = new NotificationTemplates(
                "ORDER_CONFIRMED",
                "Pedido {{pedido}} confirmado",
                "Tu pedido {{pedido}} fue confirmado"
        );

        when(templateRepository.findByTypeAndActiveTrue("ORDER_CONFIRMED"))
                .thenReturn(Optional.of(template));

        when(notificationRepository.save(any(Notifications.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationService.CreationResult result =
                notificationService.create(request, null);

        assertEquals(
                "Pedido ABC123 confirmado",
                result.notification().title()
        );

        assertEquals(
                "Tu pedido ABC123 fue confirmado",
                result.notification().content()
        );
    }

    @Test
    void create_debeRechazarPlantillaInexistente() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED",
                null, null,
                Map.of()
        );

        when(templateRepository.findByTypeAndActiveTrue("ORDER_CONFIRMED"))
                .thenReturn(Optional.empty());

        assertThrows(
                ApiException.class,
                () -> notificationService.create(request, null)
        );

        verify(notificationRepository, never())
                .save(any(Notifications.class));
    }

    @Test
    void create_debeRechazarParametroFaltante() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED",
                null, null,
                Map.of()
        );

        NotificationTemplates template = new NotificationTemplates(
                "ORDER_CONFIRMED",
                "Pedido {{pedido}}",
                "Contenido {{pedido}}"
        );

        when(templateRepository.findByTypeAndActiveTrue("ORDER_CONFIRMED"))
                .thenReturn(Optional.of(template));

        assertThrows(
                ApiException.class,
                () -> notificationService.create(request, null)
        );

        verify(notificationRepository, never())
                .save(any(Notifications.class));
    }


    @Test
    void create_debeRechazarClaveUsadaConOtroTipo() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CANCELLED", "Título", "Contenido", null
        );

        Notifications existing = new Notifications(
                "1", "Título", "Contenido", "ORDER_CONFIRMED"
        );

        when(notificationRepository.findByIdempotencyKey("evento-123"))
                .thenReturn(Optional.of(existing));

        assertThrows(
                ApiException.class,
                () -> notificationService.create(request, "evento-123")
        );

        verify(notificationRepository, never()).save(any(Notifications.class));
    }

    @Test
    void create_debeRechazarClaveUsadaConOtroTitulo() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED", "Título diferente", "Contenido", null
        );

        Notifications existing = new Notifications(
                "1", "Título original", "Contenido", "ORDER_CONFIRMED"
        );

        when(notificationRepository.findByIdempotencyKey("evento-123"))
                .thenReturn(Optional.of(existing));

        assertThrows(
                ApiException.class,
                () -> notificationService.create(request, "evento-123")
        );
    }

    @Test
    void create_debeRechazarClaveUsadaConOtroContenido() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED", "Título", "Contenido diferente", null
        );

        Notifications existing = new Notifications(
                "1", "Título", "Contenido original", "ORDER_CONFIRMED"
        );

        when(notificationRepository.findByIdempotencyKey("evento-123"))
                .thenReturn(Optional.of(existing));

        assertThrows(
                ApiException.class,
                () -> notificationService.create(request, "evento-123")
        );
    }

    @Test
    void create_debeReutilizarClaveConTituloYContenidoOmitidos() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED", null, null, null
        );

        Notifications existing = new Notifications(
                "1", "Título original", "Contenido original", "ORDER_CONFIRMED"
        );
        existing.setIdNotification(7);

        when(notificationRepository.findByIdempotencyKey("evento-123"))
                .thenReturn(Optional.of(existing));

        NotificationService.CreationResult result =
                notificationService.create(request, "evento-123");

        assertFalse(result.created());
        assertEquals(7, result.notification().id());

        verifyNoInteractions(templateRepository);
        verify(notificationRepository, never()).save(any(Notifications.class));
    }

    @Test
    void create_debeRechazarTituloGeneradoDemasiadoLargo() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1", "ORDER_CONFIRMED", null, "Contenido", Map.of()
        );

        NotificationTemplates template = new NotificationTemplates(
                "ORDER_CONFIRMED",
                "X".repeat(201),
                "Contenido de plantilla"
        );

        when(templateRepository.findByTypeAndActiveTrue("ORDER_CONFIRMED"))
                .thenReturn(Optional.of(template));

        assertThrows(
                ApiException.class,
                () -> notificationService.create(request, null)
        );

        verify(notificationRepository, never()).save(any(Notifications.class));
    }

    @Test
    void create_debeGenerarSoloContenidoCuandoTituloExiste() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                "1",
                "ORDER_CONFIRMED",
                "Mi título personalizado",
                null,
                Map.of("pedido", "ABC123")
        );

        NotificationTemplates template = new NotificationTemplates(
                "ORDER_CONFIRMED",
                "Título de plantilla",
                "Pedido {{pedido}} confirmado"
        );

        when(templateRepository.findByTypeAndActiveTrue("ORDER_CONFIRMED"))
                .thenReturn(Optional.of(template));

        when(notificationRepository.save(any(Notifications.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationService.CreationResult result =
                notificationService.create(request, null);

        assertEquals(
                "Mi título personalizado",
                result.notification().title()
        );
        assertEquals(
                "Pedido ABC123 confirmado",
                result.notification().content()
        );
    }


}
