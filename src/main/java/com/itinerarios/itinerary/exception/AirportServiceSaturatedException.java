package com.itinerarios.itinerary.exception;

/**
 * Bulkhead lleno: ya hay demasiadas validaciones en curso contra Airport Service. Es 503
 * (hereda de ExternalServiceException) pero no se reintenta ni cuenta como fallo de Airport:
 * la saturacion es local y reintentar solo agregaria carga (paso 31, seccion 108).
 */
public class AirportServiceSaturatedException extends ExternalServiceException {

    public AirportServiceSaturatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
