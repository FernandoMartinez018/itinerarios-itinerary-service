package com.itinerarios.itinerary.exception;

/**
 * Circuit Breaker abierto: la llamada ni se intento. Sigue siendo 503 (hereda de
 * ExternalServiceException) pero Retry la ignora: reintentar contra un breaker abierto
 * solo agrega latencia y anula el fail-fast (seccion 28).
 */
public class CircuitOpenException extends ExternalServiceException {

    public CircuitOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
