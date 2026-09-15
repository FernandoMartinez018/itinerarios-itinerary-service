package com.itinerarios.itinerary.event;

import com.itinerarios.itinerary.entity.Itinerary;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Espejo exacto del contrato definido en
 * itinerarios-docs/events/asyncapi.yaml. Cualquier cambio de campo aquí
 * debe sincronizarse con ese archivo, con el Consumer de Notification
 * Service, y con los tests de ambos lados (sección 100 de la
 * especificación maestra).
 */
public record ItineraryCreatedEvent(
        UUID eventId,
        String eventType,
        OffsetDateTime occurredAt,
        Long itineraryId,
        String userName,
        String departureAirportId,
        String arrivalAirportId,
        LocalDate travelDate,
        Integer durationMinutes
) {

    public static ItineraryCreatedEvent from(Itinerary itinerary) {
        return new ItineraryCreatedEvent(
                UUID.randomUUID(),
                "ItineraryCreatedEvent",
                OffsetDateTime.now(),
                itinerary.getId(),
                itinerary.getUserName(),
                itinerary.getDepartureAirportId(),
                itinerary.getArrivalAirportId(),
                itinerary.getTravelDate(),
                itinerary.getDurationMinutes()
        );
    }
}
