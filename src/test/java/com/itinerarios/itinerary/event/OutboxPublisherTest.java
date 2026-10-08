package com.itinerarios.itinerary.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itinerarios.itinerary.entity.OutboxEvent;
import com.itinerarios.itinerary.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private OutboxEvent pendingEventFor(Long itineraryId) {
        ItineraryCreatedEvent payload = new ItineraryCreatedEvent(
                UUID.randomUUID(), "ItineraryCreatedEvent", OffsetDateTime.now(),
                itineraryId, "Juan Perez", "BOG", "MDE", LocalDate.now().plusDays(5), 60
        );
        try {
            return OutboxEvent.pending("Itinerary", String.valueOf(itineraryId),
                    "ItineraryCreatedEvent", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void publishPendingEvents_publicaYMarcaComoPublishedCuandoRabbitFunciona() {
        OutboxPublisher publisher = new OutboxPublisher(outboxEventRepository, rabbitTemplate, objectMapper);
        OutboxEvent event = pendingEventFor(123L);

        when(outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(event));

        publisher.publishPendingEvents();

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(ItineraryCreatedEvent.class));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.PUBLISHED);
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void publishPendingEvents_marcaFailedYSubeRetryCountCuandoRabbitFalla() {
        OutboxPublisher publisher = new OutboxPublisher(outboxEventRepository, rabbitTemplate, objectMapper);
        OutboxEvent event = pendingEventFor(456L);

        when(outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(event));
        doThrow(new AmqpException("broker caído") {})
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(ItineraryCreatedEvent.class));

        publisher.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        assertThat(captor.getValue().getErrorMessage()).isNotBlank();
    }

    @Test
    void publishPendingEvents_noReintentaEventosQueYaAgotaronElMaximoDeReintentos() {
        OutboxPublisher publisher = new OutboxPublisher(outboxEventRepository, rabbitTemplate, objectMapper);
        OutboxEvent exhausted = pendingEventFor(789L);
        for (int i = 0; i < 5; i++) {
            exhausted.markFailed("fallo simulado " + i);
        }

        when(outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(anyList()))
                .thenReturn(List.of(exhausted));

        publisher.publishPendingEvents();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(ItineraryCreatedEvent.class));
        verify(outboxEventRepository, never()).save(any());
    }
}
