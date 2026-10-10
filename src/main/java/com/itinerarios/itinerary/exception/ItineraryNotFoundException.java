package com.itinerarios.itinerary.exception;

public class ItineraryNotFoundException extends RuntimeException {

    public ItineraryNotFoundException(Long id) {
        super("Itinerary not found with id: " + id);
    }
}
