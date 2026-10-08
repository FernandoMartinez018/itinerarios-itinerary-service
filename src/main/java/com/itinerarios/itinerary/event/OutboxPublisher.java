package com.itinerarios.itinerary.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itinerarios.itinerary.config.RabbitMQConfig;
import com.itinerarios.itinerary.entity.OutboxEvent;
import com.itinerarios.itinerary.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Segunda mitad del patrón Transactional Outbox (sección 81, ADR-008):
 * lee periódicamente las filas PENDING/FAILED de outbox_events y las publica
 * a RabbitMQ, marcando cada fila como PUBLISHED o incrementando su
 * retry_count si falla de nuevo.
 *
 * Esto corre en un proceso/hilo separado de la transacción que creó el
 * itinerario (sección 24: "Outbox Publisher" es un componente propio en el
 * diagrama de Nivel 3), por lo que un fallo de RabbitMQ aquí nunca puede
 * revertir el itinerario ya confirmado — solo demora la publicación del
 * evento, que se reintentará en la siguiente ejecución programada.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                            RabbitTemplate rabbitTemplate,
                            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms:2000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(
                List.of(OutboxEvent.Status.PENDING, OutboxEvent.Status.FAILED));

        for (OutboxEvent event : pending) {
            if (event.getRetryCount() >= MAX_RETRY_COUNT) {
                // No reintentar indefinidamente (sección 28). El evento queda
                // en FAILED con su último error_message para investigación
                // manual; no bloquea la publicación de los demás eventos.
                continue;
            }
            publishOne(event);
        }
    }

    @Transactional
    void publishOne(OutboxEvent event) {
        try {
            ItineraryCreatedEvent payload = objectMapper.readValue(event.getPayload(), ItineraryCreatedEvent.class);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ITINERARY_EVENTS_EXCHANGE,
                    RabbitMQConfig.ITINERARY_CREATED_ROUTING_KEY,
                    payload
            );
            event.markPublished();
            log.info("Outbox event {} published for itinerary {}", event.getId(), event.getAggregateId());
        } catch (AmqpException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            event.markFailed(ex.getMessage());
            log.error("Failed to publish outbox event {} (attempt {}/{})",
                    event.getId(), event.getRetryCount() + 1, MAX_RETRY_COUNT, ex);
        }
        outboxEventRepository.save(event);
    }
}
