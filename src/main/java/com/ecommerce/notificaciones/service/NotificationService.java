package com.ecommerce.notificaciones.service;

import com.ecommerce.notificaciones.dto.CreateNotificationRequest;
import com.ecommerce.notificaciones.dto.NotificationResponse;
import com.ecommerce.notificaciones.exception.ApiException;
import com.ecommerce.notificaciones.model.NotificationTemplates;
import com.ecommerce.notificaciones.model.Notifications;
import com.ecommerce.notificaciones.model.ProcessedEvents;
import com.ecommerce.notificaciones.repository.NotificationRepository;
import com.ecommerce.notificaciones.repository.NotificationTemplateRepository;
import com.ecommerce.notificaciones.repository.ProcessedEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationService {

    // Resultado: la notificación y si se creó ahora (true) o ya existía por idempotencia (false)
    public record CreationResult(NotificationResponse notification, boolean created) {}

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final ProcessedEventRepository processedEventRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationTemplateRepository templateRepository,
                               ProcessedEventRepository processedEventRepository) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public CreationResult create(CreateNotificationRequest req, String rawKey) {
        String key = (rawKey == null || rawKey.isBlank()) ? null : rawKey.trim();
        if (key != null && key.length() > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key admite máximo 120 caracteres");
        }
        String receiver = req.idReceiver().trim();
        String type = req.type().trim();

        // 1) Idempotencia: si la clave ya se usó, no se crea nada nuevo
        if (key != null) {
            Optional<Notifications> existing = notificationRepository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                Notifications n = existing.get();
                // Misma clave con datos distintos = conflicto (409)
                boolean same = n.getIdReceiver().equals(receiver) && n.getType().equals(type)
                        && (isBlank(req.title()) || n.getTitle().equals(req.title()))
                        && (isBlank(req.content()) || n.getContent().equals(req.content()));
                if (!same) {
                    throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                            "La clave de idempotencia ya se usó con una solicitud distinta");
                }
                return new CreationResult(NotificationResponse.from(n), false);
            }
        }

        // 2) Título y contenido: los que manda el cliente; los faltantes salen de la plantilla activa
        String title = req.title();
        String content = req.content();
        if (isBlank(title) || isBlank(content)) {
            NotificationTemplates t = templateRepository.findByTypeAndActiveTrue(type)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "TEMPLATE_NOT_FOUND",
                            "No hay plantilla activa para el tipo indicado"));
            if (isBlank(title)) title = render(t.getTitleTemplate(), req.params());
            if (isBlank(content)) content = render(t.getContentTemplate(), req.params());
        }
        if (title.length() > 200) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "TITLE_TOO_LONG",
                    "El título generado supera los 200 caracteres");
        }

        // 3) Se registra en PENDING (el envío lo hace otro servicio) y se marca el evento como procesado
        Notifications n = new Notifications(receiver, title, content, type);
        n.setIdempotencyKey(key);
        n = notificationRepository.save(n);
        if (key != null) {
            processedEventRepository.save(new ProcessedEvents(key, type, n));
        }
        return new CreationResult(NotificationResponse.from(n), true);
    }

    // Reemplaza {{param}} por su valor; si falta alguno -> 422
    private String render(String template, Map<String, String> params) {
        Map<String, String> p = params == null ? Map.of() : params;
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = p.get(m.group(1));
            if (value == null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "TEMPLATE_PARAM_MISSING",
                        "Falta el parámetro de plantilla: " + m.group(1));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
