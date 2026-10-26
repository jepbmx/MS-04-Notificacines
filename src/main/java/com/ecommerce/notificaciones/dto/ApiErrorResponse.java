package com.ecommerce.notificaciones.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors) {
}
