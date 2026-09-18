package com.itinerarios.itinerary.client;

import com.itinerarios.itinerary.exception.ExternalServiceException;
import com.itinerarios.itinerary.exception.InvalidAirportException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Unico punto de comunicacion con Airport Service.
 * Regla de aislamiento (seccion 102): Itinerary Service NUNCA accede
 * directamente a airport_db, siempre a traves de este cliente HTTP.
 *
 * Resiliencia (seccion 28, Fase 10): Retry + Circuit Breaker protegen esta
 * llamada. Decision importante: InvalidAirportException (HTTP 404, "el
 * aeropuerto no existe") esta configurada como excepcion IGNORADA en
 * application.yml -- un 404 es una respuesta de negocio valida, no una
 * falla del servicio, y NO debe reintentarse ni contar para abrir el
 * circuito. Solo fallas reales (timeout, conexion rechazada, 5xx) cuentan.
 */
@Component
public class AirportServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AirportServiceClient.class);
    private static final String RESILIENCE_INSTANCE = "airportService";

    private final RestClient airportServiceRestClient;

    public AirportServiceClient(RestClient airportServiceRestClient) {
        this.airportServiceRestClient = airportServiceRestClient;
    }

    /**
     * Valida que un codigo IATA exista en Airport Service.
     *
     * @throws InvalidAirportException  si el aeropuerto no existe (HTTP 404)
     * @throws ExternalServiceException si Airport Service no responde correctamente
     */
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "validateAirportFallback")
    public AirportSummary validateAirport(String iataCode) {
        try {
            return airportServiceRestClient.get()
                    .uri("/api/airports/iata/{iataCode}", iataCode)
                    .retrieve()
                    .body(AirportSummary.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new InvalidAirportException(iataCode);
        } catch (RestClientException ex) {
            log.error("Error calling Airport Service for IATA code {}", iataCode, ex);
            throw new ExternalServiceException("Airport Service unavailable", ex);
        }
    }

    @SuppressWarnings("unused")
    private AirportSummary validateAirportFallback(String iataCode, Throwable throwable) {
        if (throwable instanceof InvalidAirportException invalidAirportException) {
            // Nunca deberia llegar aqui porque InvalidAirportException esta
            // en la lista de excepciones ignoradas por el Circuit Breaker,
            // pero se relanza igual por defensividad.
            throw invalidAirportException;
        }
        log.error("Circuit breaker '{}' activo: no se pudo validar el aeropuerto {}", RESILIENCE_INSTANCE, iataCode, throwable);
        throw new ExternalServiceException("Airport Service no disponible (circuit breaker abierto)", throwable);
    }
}
