package com.itinerarios.itinerary.mapper;

import com.itinerarios.itinerary.dto.ItineraryDto;
import com.itinerarios.itinerary.entity.Itinerary;
import org.springframework.stereotype.Component;

@Component
public class ItineraryMapper {

    public ItineraryDto toDto(Itinerary itinerary) {
        return new ItineraryDto(
                itinerary.getId(),
                itinerary.getUserName(),
                itinerary.getDepartureAirportId(),
                itinerary.getArrivalAirportId(),
                itinerary.getTravelDate(),
                itinerary.getDurationMinutes(),
                itinerary.getCreatedAt(),
                itinerary.getUpdatedAt()
        );
    }
}
