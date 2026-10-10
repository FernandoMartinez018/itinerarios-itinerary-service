package com.itinerarios.itinerary.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Usado tanto para POST como para PUT (reemplazo completo).
 * La validación de existencia real de los aeropuertos (no solo formato)
 * ocurre en el Service contra Airport Service, no aquí.
 */
public record ItineraryRequest(

        @NotBlank(message = "userName is required")
        String userName,

        @NotBlank(message = "departureAirportId is required")
        String departureAirportId,

        @NotBlank(message = "arrivalAirportId is required")
        String arrivalAirportId,

        @NotNull(message = "travelDate is required")
        @FutureOrPresent(message = "travelDate must be today or in the future")
        LocalDate travelDate,

        @NotNull(message = "durationMinutes is required")
        @Positive(message = "durationMinutes must be greater than 0")
        Integer durationMinutes
) {
}
