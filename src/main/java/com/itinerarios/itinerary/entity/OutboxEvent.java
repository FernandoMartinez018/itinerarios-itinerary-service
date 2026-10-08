package com.itinerarios.itinerary.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tabla de la sección 15 de la especificación maestra. Vive en itinerary_db,
 * en la MISMA transacción que la tabla `itineraries` (ver ItineraryService.create()),
 * para resolver el Double Write Problem documentado en ADR-008: el itinerario
 * y el registro de que "hay un evento pendiente de publicar" se confirman o
 * se revierten juntos, atómicamente, porque viven en la misma base de datos.
 *
 * Un proceso separado (OutboxPublisher) lee las filas PENDING y las publica
 * a RabbitMQ de forma asíncrona, fuera de esta transacción.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    public enum Status {
        PENDING, PUBLISHED, FAILED
    }

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected OutboxEvent() {
        // requerido por JPA
    }

    public static OutboxEvent pending(String aggregateType, String aggregateId, String eventType, String payloadJson) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payloadJson;
        event.status = Status.PENDING;
        event.retryCount = 0;
        return event;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void markPublished() {
        this.status = Status.PUBLISHED;
        this.publishedAt = OffsetDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = Status.FAILED;
        this.retryCount = this.retryCount + 1;
        this.errorMessage = errorMessage;
    }

    // ---- getters ----

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Status getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
