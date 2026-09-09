package com.itinerarios.itinerary.client;

import com.itinerarios.itinerary.exception.ExternalServiceException;
import com.itinerarios.itinerary.exception.InvalidAirportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Único punto de comunicación con Airport Service.
 * Regla de aislamiento (sección 102): Itinerary Service NUNCA accede
 * directamente a airport_db, siempre a través de este cliente HTTP.
 */
@Component
public class AirportServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AirportServiceClient.class);

    private final RestClient airportServiceRestClient;

    public AirportServiceClient(RestClient airportServiceRestClient) {
        this.airportServiceRestClient = airportServiceRestClient;
    }

    /**
     * Valida que un código IATA exista en Airport Service.
     *
     * @throws InvalidAirportException  si el aeropuerto no existe (HTTP 404)
     * @throws ExternalServiceException si Airport Service no responde correctamente
     */
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
}
