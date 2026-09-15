package com.itinerarios.itinerary.event;

import com.itinerarios.itinerary.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica ItineraryCreatedEvent después de que el itinerario ya se
 * persistió exitosamente en PostgreSQL (sección 24, tramo Nivel 2).
 *
 * LIMITACIÓN CONOCIDA (Double Write Problem, sección 81): si la
 * publicación falla después de que la transacción de PostgreSQL ya hizo
 * commit, el itinerario queda guardado pero el evento se pierde — no hay
 * forma de deshacer un commit ya confirmado ni de reintentar la
 * publicación de forma transaccional en este punto. Esta clase
 * deliberadamente NO relanza la excepción para no romper la respuesta
 * HTTP de creación del itinerario por un fallo del broker; solo la
 * registra. La solución real (Transactional Outbox) es Nivel 3, sección
 * 81 — no se implementa aquí a propósito, para no adelantar niveles.
 */
@Component
public class ItineraryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ItineraryEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ItineraryCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ITINERARY_EVENTS_EXCHANGE,
                    RabbitMQConfig.ITINERARY_CREATED_ROUTING_KEY,
                    event
            );
        } catch (AmqpException ex) {
            log.error("Failed to publish ItineraryCreatedEvent for itinerary {}. "
                    + "The itinerary was already persisted; the event was lost. "
                    + "See ADR-008 (Transactional Outbox, Nivel 3) for the real fix.",
                    event.itineraryId(), ex);
        }
    }
}
