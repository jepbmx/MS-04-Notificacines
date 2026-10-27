package com.ecommerce.notificaciones.exception;

import java.time.Instant;

// Formato común de error del PDF
public record ErrorResponse(Instant timestamp, int status, String code,
                            String message, String path) {}