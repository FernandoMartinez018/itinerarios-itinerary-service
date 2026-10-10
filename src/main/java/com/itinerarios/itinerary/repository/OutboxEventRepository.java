package com.itinerarios.itinerary.repository;

import com.itinerarios.itinerary.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Trae eventos listos para (re)intentar publicación: PENDING o FAILED cuyo
     * backoff ya venció (next_attempt_at nulo o pasado). Ordenados por antigüedad
     * para publicar en el mismo orden en que se crearon los itinerarios.
     */
    @Query("""
            select e from OutboxEvent e
            where e.status in :statuses and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
            order by e.createdAt asc""")
    List<OutboxEvent> findReadyToPublish(@Param("statuses") List<OutboxEvent.Status> statuses,
                                         @Param("now") OffsetDateTime now, Limit limit);
}
