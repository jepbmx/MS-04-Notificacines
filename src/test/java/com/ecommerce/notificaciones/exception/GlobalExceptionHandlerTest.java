package com.ecommerce.notificaciones.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsApiAndValidationErrorsToStructuredResponses() {
        MockHttpServletRequest request = request();
        ApiException apiException =
                new ApiException(HttpStatus.CONFLICT, "CONFLICT", "Duplicate");

        var apiResponse = handler.handleApi(apiException, request);
        assertEquals(HttpStatus.CONFLICT, apiResponse.getStatusCode());
        assertEquals("CONFLICT", apiResponse.getBody().code());
        assertEquals("Duplicate", apiResponse.getBody().message());
        assertEquals("/api/test", apiResponse.getBody().path());
        assertEquals(HttpStatus.CONFLICT, apiException.getStatus());
        assertEquals("CONFLICT", apiException.getCode());

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
                "request", "title", "must not be blank"
        ));
        MethodArgumentNotValidException validationException =
                new MethodArgumentNotValidException(null, bindingResult);
        var validationResponse =
                handler.handleValidation(validationException, request);

        assertEquals(HttpStatus.BAD_REQUEST, validationResponse.getStatusCode());
        assertEquals("VALIDATION_ERROR", validationResponse.getBody().code());
        assertEquals("title: must not be blank", validationResponse.getBody().message());
    }

    @Test
    void mapsMalformedAndIntegrityErrors() {
        MockHttpServletRequest request = request();
        HttpMessageNotReadableException malformed =
                new HttpMessageNotReadableException(
                        "bad json",
                        new MockHttpInputMessage("{".getBytes(StandardCharsets.UTF_8))
                );

        var malformedResponse = handler.handleMalformed(malformed, request);
        assertEquals(HttpStatus.BAD_REQUEST, malformedResponse.getStatusCode());
        assertEquals("MALFORMED_JSON", malformedResponse.getBody().code());
        assertEquals(
                "El cuerpo de la solicitud no es un JSON válido",
                malformedResponse.getBody().message()
        );

        var integrityResponse = handler.handleIntegrity(
                new DataIntegrityViolationException("duplicate"), request
        );
        assertEquals(HttpStatus.CONFLICT, integrityResponse.getStatusCode());
        assertEquals("DUPLICATE_RESOURCE", integrityResponse.getBody().code());
    }

    @Test
    void preservesSpringHttpStatusAndHidesUnexpectedErrorDetails() {
        MockHttpServletRequest request = request();
        ResponseStatusException notFound =
                new ResponseStatusException(HttpStatus.NOT_FOUND, "private detail");

        var httpResponse = handler.handleUnexpected(notFound, request);
        assertEquals(HttpStatus.NOT_FOUND, httpResponse.getStatusCode());
        assertEquals("HTTP_ERROR", httpResponse.getBody().code());
        assertEquals("No se pudo procesar la solicitud", httpResponse.getBody().message());

        var unexpectedResponse =
                handler.handleUnexpected(new IllegalStateException("secret"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, unexpectedResponse.getStatusCode());
        assertEquals("INTERNAL_ERROR", unexpectedResponse.getBody().code());
        assertEquals("Ocurrió un error inesperado", unexpectedResponse.getBody().message());

        ErrorResponse error = new ErrorResponse(
                Instant.parse("2026-01-01T00:00:00Z"),
                400, "BAD_REQUEST", "invalid", "/api/test"
        );
        assertEquals(400, error.status());
        assertEquals("BAD_REQUEST", error.code());
        assertEquals("invalid", error.message());
        assertEquals("/api/test", error.path());
        assertEquals(error, new ErrorResponse(
                error.timestamp(), error.status(), error.code(),
                error.message(), error.path()
        ));
        assertEquals(error.hashCode(), new ErrorResponse(
                error.timestamp(), error.status(), error.code(),
                error.message(), error.path()
        ).hashCode());
        assertTrue(error.toString().contains("BAD_REQUEST"));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        return request;
    }
}
