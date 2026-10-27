package com.ecommerce.notificaciones.exception;

import org.springframework.http.HttpStatus;

// Error de negocio con status HTTP y código propio (se serializa con el formato común de error)
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}