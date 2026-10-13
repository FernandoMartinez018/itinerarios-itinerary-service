package com.itinerarios.itinerary.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Subconjunto del AirportDto expuesto por itinerarios-airport-service.
 * Solo se toman los campos que Itinerary Service realmente necesita
 * para validar un código IATA (sección 11: Itinerary no mantiene una
 * entidad Airport completa).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AirportSummary(
        Long id,
        String iataCode,
        String name,
        boolean active
) {
}
