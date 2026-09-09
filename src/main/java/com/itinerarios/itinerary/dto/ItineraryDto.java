package com.itinerarios.itinerary.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ItineraryDto(
        Long id,
        String userName,
        String departureAirportId,
        String arrivalAirportId,
        LocalDate travelDate,
        Integer durationMinutes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
