package com.itinerarios.itinerary.repository;

import com.itinerarios.itinerary.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Trae eventos listos para (re)intentar publicación: los que nunca se
     * intentaron (PENDING) y los que fallaron pero todavía no agotaron el
     * máximo de reintentos. Ordenados por antigüedad para publicar en el
     * mismo orden en que se crearon los itinerarios.
     */
    List<OutboxEvent> findTop50ByStatusInOrderByCreatedAtAsc(List<OutboxEvent.Status> statuses);
}
