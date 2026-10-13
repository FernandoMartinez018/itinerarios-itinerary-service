package com.itinerarios.itinerary.controller;

import com.itinerarios.itinerary.dto.ItineraryDto;
import com.itinerarios.itinerary.exception.ExternalServiceException;
import com.itinerarios.itinerary.exception.InvalidAirportException;
import com.itinerarios.itinerary.exception.ItineraryNotFoundException;
import com.itinerarios.itinerary.service.ItineraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests de API (seccion 37): codigos HTTP, validaciones y JSON de error (seccion 64). */
@WebMvcTest(ItineraryController.class)
class ItineraryControllerTest {

    private static final String VALID_BODY = "{\"userName\":\"Juan Perez\",\"departureAirportId\":\"BOG\","
            + "\"arrivalAirportId\":\"MDE\",\"travelDate\":\"" + LocalDate.now().plusDays(5) + "\",\"durationMinutes\":60}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItineraryService itineraryService;

    private ItineraryDto sample() {
        return new ItineraryDto(1L, "Juan Perez", "BOG", "MDE", LocalDate.now().plusDays(5), 60,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void getAll_devuelve200() throws Exception {
        when(itineraryService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/itineraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].departureAirportId").value("BOG"));
    }

    @Test
    void getById_inexistente_devuelve404ConJsonDeError() throws Exception {
        when(itineraryService.findById(99L)).thenThrow(new ItineraryNotFoundException(99L));

        mockMvc.perform(get("/api/itineraries/99").header("X-Correlation-ID", "abc-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/itineraries/99"))
                .andExpect(jsonPath("$.correlationId").value("abc-123"));
    }

    @Test
    void post_valido_devuelve201() throws Exception {
        when(itineraryService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/itineraries").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.travelDate").isString());
    }

    @Test
    void post_duracionNoPositiva_devuelve400SinLlamarAlService() throws Exception {
        String body = VALID_BODY.replace("\"durationMinutes\":60", "\"durationMinutes\":0");

        mockMvc.perform(post("/api/itineraries").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verifyNoInteractions(itineraryService);
    }

    @Test
    void post_aeropuertoInexistente_devuelve400() throws Exception {
        when(itineraryService.create(any())).thenThrow(new InvalidAirportException("XXX"));

        mockMvc.perform(post("/api/itineraries").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_AIRPORT"));
    }

    @Test
    void post_airportServiceCaido_devuelve503() throws Exception {
        when(itineraryService.create(any())).thenThrow(new ExternalServiceException("down", null));

        mockMvc.perform(post("/api/itineraries").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void put_valido_devuelve200() throws Exception {
        when(itineraryService.update(eq(1L), any())).thenReturn(sample());

        mockMvc.perform(put("/api/itineraries/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_existente_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/itineraries/1"))
                .andExpect(status().isNoContent());
        verify(itineraryService).delete(1L);
    }

    @Test
    void errorInesperado_devuelve500SinFiltrarDetalles() throws Exception {
        when(itineraryService.findAll()).thenThrow(new IllegalStateException("detalle interno"));

        mockMvc.perform(get("/api/itineraries"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }

    @Test
    void post_jsonMalformado_devuelve400() throws Exception {
        mockMvc.perform(post("/api/itineraries").contentType(MediaType.APPLICATION_JSON).content("{malformado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void getById_noNumerico_devuelve400() throws Exception {
        mockMvc.perform(get("/api/itineraries/abc"))
                .andExpect(status().isBadRequest());
    }
}
