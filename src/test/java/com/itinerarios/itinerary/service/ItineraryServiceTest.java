package com.itinerarios.itinerary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itinerarios.itinerary.client.AirportServiceClient;
import com.itinerarios.itinerary.client.AirportSummary;
import com.itinerarios.itinerary.dto.ItineraryDto;
import com.itinerarios.itinerary.dto.ItineraryRequest;
import com.itinerarios.itinerary.entity.Itinerary;
import com.itinerarios.itinerary.entity.OutboxEvent;
import com.itinerarios.itinerary.exception.InvalidAirportException;
import com.itinerarios.itinerary.exception.ItineraryNotFoundException;
import com.itinerarios.itinerary.mapper.ItineraryMapper;
import com.itinerarios.itinerary.repository.ItineraryRepository;
import com.itinerarios.itinerary.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    @Mock
    private ItineraryRepository itineraryRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AirportServiceClient airportServiceClient;

    private final ItineraryMapper mapper = new ItineraryMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private ItineraryService itineraryService;

    private ItineraryRequest validRequest() {
        return new ItineraryRequest("Juan Perez", "BOG", "MDE", LocalDate.now().plusDays(5), 60);
    }

    @Test
    void create_validaAmbosAeropuertosYPersisteCuandoSonValidos() {
        when(airportServiceClient.validateAirport("BOG")).thenReturn(new AirportSummary(1L, "BOG", "El Dorado", true));
        when(airportServiceClient.validateAirport("MDE")).thenReturn(new AirportSummary(2L, "MDE", "José María Córdova", true));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        ItineraryDto result = itineraryService.create(validRequest());

        assertThat(result.departureAirportId()).isEqualTo("BOG");
        assertThat(result.arrivalAirportId()).isEqualTo("MDE");
        verify(airportServiceClient).validateAirport("BOG");
        verify(airportServiceClient).validateAirport("MDE");
        verify(itineraryRepository, times(1)).save(any(Itinerary.class));
    }

    @Test
    void create_escribeUnOutboxEventPendingEnLaMismaOperacion() {
        when(airportServiceClient.validateAirport(anyString()))
                .thenReturn(new AirportSummary(1L, "BOG", "El Dorado", true));
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        itineraryService.create(validRequest());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
        assertThat(saved.getAggregateType()).isEqualTo("Itinerary");
        assertThat(saved.getEventType()).isEqualTo("ItineraryCreatedEvent");
        assertThat(saved.getPayload()).contains("BOG").contains("MDE");
    }

    @Test
    void create_fallaCuandoAeropuertoDeSalidaNoExiste() {
        when(airportServiceClient.validateAirport("BOG")).thenThrow(new InvalidAirportException("BOG"));

        assertThatThrownBy(() -> itineraryService.create(validRequest()))
                .isInstanceOf(InvalidAirportException.class);

        verify(itineraryRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void findById_lanzaExcepcionCuandoNoExiste() {
        when(itineraryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itineraryService.findById(1L))
                .isInstanceOf(ItineraryNotFoundException.class);
    }

    @Test
    void delete_lanzaExcepcionCuandoNoExiste() {
        when(itineraryRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> itineraryService.delete(1L))
                .isInstanceOf(ItineraryNotFoundException.class);

        verify(itineraryRepository, never()).deleteById(any());
    }
}
