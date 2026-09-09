package com.itinerarios.itinerary.repository;

import com.itinerarios.itinerary.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
}
