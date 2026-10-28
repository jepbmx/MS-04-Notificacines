package com.ecommerce.notificaciones.controller;

import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.model.NotificationStatus;
import com.ecommerce.notificaciones.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerEdgeCasesTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("user@example.test")
                .claim("userId", 42L)
                .build();
    }

    private NotificationResponse response() {
        return new NotificationResponse(
                7, "42", "title", "content", "ORDER", null, false,
                Instant.parse("2026-01-01T00:00:00Z"), null, null
        );
    }

    @Test
    void createReturnsCreatedForNewNotificationAndOkForIdempotentReplay() {
        var request = new com.ecommerce.notificaciones.dto.CreateNotificationRequest(
                "42", "ORDER", "title", "content", null
        );
        when(notificationService.create(request, "key"))
                .thenReturn(new NotificationService.CreationResult(response(), true));

        var created = controller.create(request, "key");
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals("/api/notifications/7", created.getHeaders().getLocation().toString());
        assertEquals(7, created.getBody().id());

        when(notificationService.create(request, "key"))
                .thenReturn(new NotificationService.CreationResult(response(), false));
        var replay = controller.create(request, "key");
        assertEquals(HttpStatus.OK, replay.getStatusCode());
        assertEquals(7, replay.getBody().id());
    }

    @Test
    void searchAppliesDefaultSortForNullAndEmptySortLists() {
        when(notificationService.searchMyNotifications(
                eq("42"), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(Page.empty());

        controller.searchMyNotifications(jwt(), null, null, null, 0, 10, null);
        controller.searchMyNotifications(jwt(), null, null, null, 0, 10, new String[0]);

        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(notificationService, times(2)).searchMyNotifications(
                eq("42"), isNull(), isNull(), isNull(), captor.capture()
        );
        for (Pageable pageable : captor.getAllValues()) {
            assertEquals("createdAt", pageable.getSort().iterator().next().getProperty());
            assertEquals("idNotification", pageable.getSort().getOrderFor("idNotification").getProperty());
        }
    }

    @Test
    void searchAllowsSortPropertyWithoutExplicitDirection() {
        when(notificationService.searchMyNotifications(
                eq("42"), isNull(), eq(NotificationStatus.PENDING), isNull(), any(Pageable.class)
        )).thenReturn(Page.empty());

        controller.searchMyNotifications(
                jwt(), null, NotificationStatus.PENDING, null, 0, 10,
                new String[]{"status"}
        );

        verify(notificationService).searchMyNotifications(
                eq("42"), isNull(), eq(NotificationStatus.PENDING), isNull(),
                argThat(pageable -> pageable.getSort()
                        .getOrderFor("status").isAscending())
        );
    }

    @Test
    void searchRejectsInvalidPageAndSortValues() {
        assertBadRequest(() ->
                controller.searchMyNotifications(jwt(), null, null, null, -1, 10, null));
        assertBadRequest(() ->
                controller.searchMyNotifications(jwt(), null, null, null, 0, 0, null));
        assertBadRequest(() ->
                controller.searchMyNotifications(jwt(), null, null, null, 0, 10, new String[]{",asc"}));
        assertBadRequest(() ->
                controller.searchMyNotifications(jwt(), null, null, null, 0, 10, new String[]{"type,asc,read"}));
        assertBadRequest(() ->
                controller.searchMyNotifications(jwt(), null, null, null, 0, 10, new String[]{"unknown"}));
        assertBadRequest(() ->
                controller.searchMyNotifications(jwt(), null, null, null, 0, 10, new String[]{"type,sideways"}));

        when(notificationService.searchMyNotifications(
                eq("42"), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(Page.empty());
        assertDoesNotThrow(() ->
                controller.searchMyNotifications(
                        jwt(), null, null, null, 0, 10, new String[]{"idNotification,asc"}
                ));
        verify(notificationService).searchMyNotifications(
                eq("42"), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getSort().getOrderFor("idNotification").isAscending())
        );
    }

    @Test
    void getByIdRejectsNullAndNonPositiveIds() {
        assertBadRequest(() -> controller.getById(null, jwt()));
        assertBadRequest(() -> controller.getById(0, jwt()));
        assertBadRequest(() -> controller.getById(-1, jwt()));
        verifyNoInteractions(notificationService);
    }

    @Test
    void methodsWithRequiredJwtClaimRejectMissingUserId() {
        Jwt missingUserId = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("user@example.test")
                .build();

        assertThrows(AssertionError.class, () ->
                controller.getMyNotifications(missingUserId, 0, 10));
        assertThrows(AssertionError.class, () ->
                controller.markAsRead(1, missingUserId));
        verifyNoInteractions(notificationService);
    }

    private void assertBadRequest(org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, action);
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
