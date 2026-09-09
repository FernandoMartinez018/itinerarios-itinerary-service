package com.itinerarios.itinerary.exception;

/**
 * Se lanza cuando Airport Service no responde o responde con error 5xx.
 * Se traduce a HTTP 503 (Retry/Circuit Breaker llegan en Nivel 2, sección 28).
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
