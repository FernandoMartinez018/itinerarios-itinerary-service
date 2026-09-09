package com.itinerarios.itinerary.exception;

import com.itinerarios.itinerary.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ItineraryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ItineraryNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidAirportException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAirport(InvalidAirportException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_AIRPORT", ex.getMessage(), request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "AIRPORT_SERVICE_UNAVAILABLE", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Unexpected error", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, HttpServletRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(), status.value(), error, message, request.getRequestURI(), correlationId
        );
        return ResponseEntity.status(status).body(body);
    }
}
