package com.itinerarios.itinerary.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.itinerarios.itinerary.client.AirportServiceClient;
import com.itinerarios.itinerary.dto.ItineraryDto;
import com.itinerarios.itinerary.dto.ItineraryRequest;
import com.itinerarios.itinerary.entity.Itinerary;
import com.itinerarios.itinerary.entity.OutboxEvent;
import com.itinerarios.itinerary.event.ItineraryCreatedEvent;
import com.itinerarios.itinerary.exception.ItineraryNotFoundException;
import com.itinerarios.itinerary.mapper.ItineraryMapper;
import com.itinerarios.itinerary.repository.ItineraryRepository;
import com.itinerarios.itinerary.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Flujo de creacion (seccion 24, tramo Nivel 3 - Transactional Outbox,
 * ADR-008): dentro de UNA sola transaccion de PostgreSQL se escriben tanto
 * el itinerario como el evento pendiente de publicar. O ambas escrituras se
 * confirman juntas, o ninguna - nunca queda un itinerario guardado con el
 * evento perdido (el problema que Nivel 2 aceptaba como limitacion conocida
 * en ItineraryEventPublisher, ahora resuelto).
 *
 * La publicacion real a RabbitMQ ocurre despues, de forma asincrona, en
 * OutboxPublisher - un proceso separado que lee esta misma tabla.
 */
@Service
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AirportServiceClient airportServiceClient;
    private final ItineraryMapper mapper;
    private final ObjectMapper objectMapper;

    public ItineraryService(ItineraryRepository itineraryRepository,
                             OutboxEventRepository outboxEventRepository,
                             AirportServiceClient airportServiceClient,
                             ItineraryMapper mapper,
                             ObjectMapper objectMapper) {
        this.itineraryRepository = itineraryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.airportServiceClient = airportServiceClient;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ItineraryDto> findAll() {
        return itineraryRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItineraryDto findById(Long id) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ItineraryNotFoundException(id));
        return mapper.toDto(itinerary);
    }

    @Transactional
    public ItineraryDto create(ItineraryRequest request) {
        validateAirports(request.departureAirportId(), request.arrivalAirportId());

        Itinerary itinerary = new Itinerary(
                request.userName(),
                request.departureAirportId(),
                request.arrivalAirportId(),
                request.travelDate(),
                request.durationMinutes()
        );

        Itinerary saved = itineraryRepository.save(itinerary);

        // Misma transaccion que el save de arriba: si esto falla, el save
        // del itinerario tambien se revierte (rollback conjunto).
        enqueueItineraryCreatedEvent(saved);

        return mapper.toDto(saved);
    }

    @Transactional
    public ItineraryDto update(Long id, ItineraryRequest request) {
        Itinerary itinerary = itineraryRepository.findById(id)
                .orElseThrow(() -> new ItineraryNotFoundException(id));

        validateAirports(request.departureAirportId(), request.arrivalAirportId());

        itinerary.update(
                request.userName(),
                request.departureAirportId(),
                request.arrivalAirportId(),
                request.travelDate(),
                request.durationMinutes()
        );

        return mapper.toDto(itinerary);
    }

    @Transactional
    public void delete(Long id) {
        if (!itineraryRepository.existsById(id)) {
            throw new ItineraryNotFoundException(id);
        }
        itineraryRepository.deleteById(id);
    }

    private void enqueueItineraryCreatedEvent(Itinerary itinerary) {
        ItineraryCreatedEvent event = ItineraryCreatedEvent.from(itinerary);
        String payloadJson = serialize(event);

        OutboxEvent outboxEvent = OutboxEvent.pending(
                "Itinerary",
                String.valueOf(itinerary.getId()),
                event.eventType(),
                payloadJson
        );

        outboxEventRepository.save(outboxEvent);
    }

    private String serialize(ItineraryCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException ex) {
            // Un evento que no se puede serializar es un bug de programacion
            // (el record es fijo y conocido), no una falla transitoria de
            // infraestructura - se propaga y revierte toda la transaccion
            // en vez de guardar un itinerario con un evento corrupto.
            throw new IllegalStateException("Failed to serialize ItineraryCreatedEvent", ex);
        }
    }

    private void validateAirports(String departureAirportId, String arrivalAirportId) {
        airportServiceClient.validateAirport(departureAirportId);
        airportServiceClient.validateAirport(arrivalAirportId);
    }
}
