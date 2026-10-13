package com.itinerarios.itinerary.exception;

/**
 * Se lanza cuando un código de aeropuerto (departure/arrival) no existe
 * según Airport Service. Se traduce a HTTP 400 (sección 14: validación
 * obligatoria de aeropuertos antes de crear/actualizar un itinerario).
 */
public class InvalidAirportException extends RuntimeException {

    public InvalidAirportException(String iataCode) {
        super("Airport does not exist: " + iataCode);
    }
}
