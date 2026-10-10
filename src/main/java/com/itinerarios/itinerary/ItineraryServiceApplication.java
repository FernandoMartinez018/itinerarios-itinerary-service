package com.itinerarios.itinerary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling habilita OutboxPublisher (@Scheduled), que publica
// periodicamente los eventos pendientes de la tabla outbox_events (Fase 12,
// Transactional Outbox).
@EnableScheduling
@SpringBootApplication
public class ItineraryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItineraryServiceApplication.class, args);
    }
}
