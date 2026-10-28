package com.ecommerce.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

// Entrada de POST /api/notifications.
// title/content son opcionales: si faltan, se rellenan con la plantilla del type usando params.
public record CreateNotificationRequest(
        @NotBlank(message = "El destinatario es obligatorio")
        @Size(max = 100, message = "Máximo 100 caracteres")
        @Schema(example = "4")
        String idReceiver,

        @NotBlank(message = "El tipo es obligatorio")
        @Size(max = 50, message = "Máximo 50 caracteres")
        @Schema(example = "ORDER_CONFIRMED", description = "DEMO_FAIL_ONCE y DEMO_ALWAYS_FAIL solo simulan fallos si demo-types esta activo")
        String type,

        @Size(max = 200, message = "Máximo 200 caracteres")
        @Schema(example = "Pedido PED-4001 confirmado")
        String title,

        @Size(max = 5000, message = "Máximo 5000 caracteres")
        @Schema(example = "Tu pedido esta listo para entrega.")
        String content,

        @Size(max = 20, message = "Máximo 20 parámetros")
        Map<String, String> params
) {}
