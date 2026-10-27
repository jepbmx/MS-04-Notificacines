package com.ecommerce.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

// Entrada de POST /api/notifications.
// title/content son opcionales: si faltan, se rellenan con la plantilla del type usando params.
public record CreateNotificationRequest(
        @NotBlank(message = "El destinatario es obligatorio")
        @Size(max = 100, message = "Máximo 100 caracteres")
        String idReceiver,

        @NotBlank(message = "El tipo es obligatorio")
        @Size(max = 50, message = "Máximo 50 caracteres")
        String type,

        @Size(max = 200, message = "Máximo 200 caracteres")
        String title,

        @Size(max = 5000, message = "Máximo 5000 caracteres")
        String content,

        @Size(max = 20, message = "Máximo 20 parámetros")
        Map<String, String> params
) {}