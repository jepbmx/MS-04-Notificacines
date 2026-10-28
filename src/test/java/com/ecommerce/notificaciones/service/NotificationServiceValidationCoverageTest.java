package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.repository.NotificationRepository;
import com.ecommerce.notificaciones.repository.NotificationTemplateRepository;
import com.ecommerce.notificaciones.repository.ProcessedEventRepository;
import com.ecommerce.notificaciones.dto.CreateNotificationRequest;
import com.ecommerce.notificaciones.model.NotificationTemplates;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NotificationServiceValidationCoverageTest {

    private final NotificationRepository notificationRepository =
            mock(NotificationRepository.class);
    private final NotificationTemplateRepository templateRepository =
            mock(NotificationTemplateRepository.class);
    private final NotificationService service = new NotificationService(
            notificationRepository,
            templateRepository,
            mock(ProcessedEventRepository.class), mock(NotificationDeliveryService.class)
    );

    @Test
    void rejectsBlankReceiverForSearchAndLookupBeforeRepositoryAccess() {
        var pageable = PageRequest.of(0, 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.searchMyNotifications(null, null, null, null, pageable)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.searchMyNotifications(" ", null, null, null, pageable)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getNotificationById(1, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getNotificationById(1, " ")
        );
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void normalizesNullAndBlankTypesAndRendersTemplateWithoutParameters() {
        var pageable = PageRequest.of(0, 10);
        when(notificationRepository.searchByReceiverAndFilters(
                "42", null, null, null, pageable
        )).thenReturn(Page.empty(pageable));

        service.searchMyNotifications("42", null, null, null, pageable);
        service.searchMyNotifications("42", "  ", null, null, pageable);
        verify(notificationRepository, times(2)).searchByReceiverAndFilters(
                "42", null, null, null, pageable
        );

        when(templateRepository.findByTypeAndActiveTrue("ORDER"))
                .thenReturn(java.util.Optional.of(new NotificationTemplates(
                        "ORDER", "Plain title", "Plain content"
                )));
        when(notificationRepository.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0));

        var result = service.create(
                new CreateNotificationRequest("42", "ORDER", "", "", null),
                null
        );
        assertEquals("Plain title", result.notification().title());
        assertEquals("Plain content", result.notification().content());
    }
}
