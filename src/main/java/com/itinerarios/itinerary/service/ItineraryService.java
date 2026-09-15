package com.itinerarios.itinerary.service;

import com.itinerarios.itinerary.client.AirportServiceClient;
import com.itinerarios.itinerary.dto.ItineraryDto;
import com.itinerarios.itinerary.dto.ItineraryRequest;
import com.itinerarios.itinerary.entity.Itinerary;
import com.itinerarios.itinerary.event.ItineraryCreatedEvent;
import com.itinerarios.itinerary.event.ItineraryEventPublisher;
import com.itinerarios.itinerary.exception.ItineraryNotFoundException;
import com.itinerarios.itinerary.mapper.ItineraryMapper;
import com.itinerarios.itinerary.repository.ItineraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Flujo de creación (sección 24, Nivel 2):
 *
 *   validate departure airport -> Airport Service
 *   validate arrival airport   -> Airport Service
 *   save -> PostgreSQL
 *   publish ItineraryCreatedEvent -> RabbitMQ
 */
@Service
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final AirportServiceClient airportServiceClient;
    private final ItineraryEventPublisher eventPublisher;
    private final ItineraryMapper mapper;

    public ItineraryService(ItineraryRepository itineraryRepository,
                             AirportServiceClient airportServiceClient,
                             ItineraryEventPublisher eventPublisher,
                             ItineraryMapper mapper) {
        this.itineraryRepository = itineraryRepository;
        this.airportServiceClient = airportServiceClient;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
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

        // Publicación best-effort tras el save (ver limitación documentada en
        // ItineraryEventPublisher: esto no es transaccional con PostgreSQL).
        eventPublisher.publish(ItineraryCreatedEvent.from(saved));

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

    private void validateAirports(String departureAirportId, String arrivalAirportId) {
        airportServiceClient.validateAirport(departureAirportId);
        airportServiceClient.validateAirport(arrivalAirportId);
    }
}
