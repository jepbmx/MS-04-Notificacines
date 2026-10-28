
package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.dto.CreateNotificationRequest;
import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.dto.NotificationStatusResponse;
import com.ecommerce.notificaciones.exception.ApiException;
import com.ecommerce.notificaciones.model.NotificationTemplates;
import com.ecommerce.notificaciones.model.NotificationStatus;
import com.ecommerce.notificaciones.model.Notifications;
import com.ecommerce.notificaciones.model.ProcessedEvents;
import com.ecommerce.notificaciones.repository.NotificationRepository;
import com.ecommerce.notificaciones.repository.NotificationTemplateRepository;
import com.ecommerce.notificaciones.repository.ProcessedEventRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationService {

    public record CreationResult(
            NotificationResponse notification,
            boolean created
    ) {}

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final ProcessedEventRepository processedEventRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationTemplateRepository templateRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.processedEventRepository = processedEventRepository;
    }

    // POST /api/notifications
    @Transactional
    public CreationResult create(CreateNotificationRequest req, String rawKey) {

        String key = (rawKey == null || rawKey.isBlank())
                ? null
                : rawKey.trim();

        if (key != null && key.length() > 120) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key admite máximo 120 caracteres"
            );
        }

        String receiver = req.idReceiver().trim();
        String type = req.type().trim();

        // Si la clave ya existe, se devuelve la notificación anterior
        if (key != null) {
            Optional<Notifications> existing =
                    notificationRepository.findByIdempotencyKey(key);

            if (existing.isPresent()) {
                Notifications n = existing.get();

                boolean same =
                        n.getIdReceiver().equals(receiver)
                                && n.getType().equals(type)
                                && (isBlank(req.title())
                                || n.getTitle().equals(req.title()))
                                && (isBlank(req.content())
                                || n.getContent().equals(req.content()));

                if (!same) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "IDEMPOTENCY_KEY_CONFLICT",
                            "La clave de idempotencia ya se usó con una solicitud distinta"
                    );
                }

                return new CreationResult(
                        NotificationResponse.from(n),
                        false
                );
            }
        }

        // Obtener título y contenido desde la solicitud o plantilla
        String title = req.title();
        String content = req.content();

        if (isBlank(title) || isBlank(content)) {
            NotificationTemplates template =
                    templateRepository.findByTypeAndActiveTrue(type)
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.UNPROCESSABLE_CONTENT,
                                    "TEMPLATE_NOT_FOUND",
                                    "No hay plantilla activa para el tipo indicado"
                            ));

            if (isBlank(title)) {
                title = render(
                        template.getTitleTemplate(),
                        req.params()
                );
            }

            if (isBlank(content)) {
                content = render(
                        template.getContentTemplate(),
                        req.params()
                );
            }
        }

        if (title.length() > 200) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "TITLE_TOO_LONG",
                    "El título generado supera los 200 caracteres"
            );
        }

        // Registrar la notificación y su clave de idempotencia
        Notifications notification =
                new Notifications(receiver, title, content, type);

        notification.setIdempotencyKey(key);
        notification = notificationRepository.save(notification);

        if (key != null) {
            processedEventRepository.save(
                    new ProcessedEvents(key, type, notification)
            );
        }

        return new CreationResult(
                NotificationResponse.from(notification),
                true
        );
    }

    // GET /api/notifications/me
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(
            String idReceiver,
            Pageable pageable
    ) {
        if (idReceiver == null || idReceiver.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del usuario es obligatorio"
            );
        }

        return notificationRepository
                .findByIdReceiver(idReceiver, pageable)
                .map(NotificationResponse::from);
    }

    // GET /api/notifications?type=...&status=...&read=...
    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchMyNotifications(
            String idReceiver,
            String type,
            NotificationStatus status,
            Boolean read,
            Pageable pageable
    ) {
        if (idReceiver == null || idReceiver.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del usuario es obligatorio"
            );
        }

        String normalizedType = type == null || type.isBlank()
                ? null
                : type.trim();
        return notificationRepository.searchByReceiverAndFilters(
                        idReceiver, normalizedType, status, read, pageable
                )
                .map(NotificationResponse::from);
    }

    // GET /api/notifications/{id}
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(
            Integer idNotification,
            String idReceiver
    ) {
        if (idReceiver == null || idReceiver.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del usuario es obligatorio"
            );
        }

        return notificationRepository
                .findByIdNotificationAndIdReceiver(idNotification, idReceiver)
                .map(NotificationResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notificación no encontrada"
                ));
    }

    // PATCH /api/notifications/{id}/read
    @Transactional
    public NotificationResponse markNotificationAsRead(
            Integer idNotification,
            String idReceiver
    ) {
        if (idReceiver == null || idReceiver.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del usuario es obligatorio"
            );
        }

        Notifications notification = notificationRepository
                .findById(idNotification)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND",
                        "Notificación no encontrada"
                ));

        if (!notification.getIdReceiver().equals(idReceiver)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOTIFICATION_NOT_FOUND",
                    "Notificación no encontrada"
            );
        }

        notification.markAsRead();

        return NotificationResponse.from(notification);
    }

    // Sustituye los parámetros de las plantillas
    private String render(
            String template,
            Map<String, String> params
    ) {
        Map<String, String> values =
                params == null ? Map.of() : params;

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String value = values.get(matcher.group(1));

            if (value == null) {
                throw new ApiException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "TEMPLATE_PARAM_MISSING",
                        "Falta el parámetro de plantilla: "
                                + matcher.group(1)
                );
            }

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(value)
            );
        }

        matcher.appendTail(result);
        return result.toString();
    }

    // GET /api/notifications/{id}/status
    @Transactional(readOnly = true)
    public NotificationStatusResponse getNotificationStatus(
            Integer idNotification,
            String idReceiver
    ) {
        if (idReceiver == null || idReceiver.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del usuario es obligatorio"
            );
        }

        Notifications notification = notificationRepository
                .findByIdNotificationAndIdReceiver(idNotification, idReceiver)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND",
                        "Notificación no encontrada"
                ));

        return NotificationStatusResponse.from(notification);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
