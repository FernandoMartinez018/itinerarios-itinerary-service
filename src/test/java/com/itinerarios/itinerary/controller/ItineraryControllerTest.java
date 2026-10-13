package com.itinerarios.itinerary.controller;

import com.itinerarios.itinerary.config.SecurityConfig;
import com.itinerarios.itinerary.dto.ItineraryDto;
import com.itinerarios.itinerary.dto.PageResponse;
import com.itinerarios.itinerary.exception.ExternalServiceException;
import com.itinerarios.itinerary.exception.InvalidAirportException;
import com.itinerarios.itinerary.exception.ItineraryNotFoundException;
import com.itinerarios.itinerary.service.ItineraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests de API (seccion 37): codigos HTTP, validaciones y JSON de error (seccion 64). */
@WebMvcTest(ItineraryController.class)
@Import(SecurityConfig.class)
class ItineraryControllerTest {

    private static JwtRequestPostProcessor reader() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_itinerary.read"));
    }

    private static JwtRequestPostProcessor writer() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_itinerary.write"));
    }

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

        mockMvc.perform(get("/api/itineraries").with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].departureAirportId").value("BOG"));
    }

    @Test
    void getById_inexistente_devuelve404ConJsonDeError() throws Exception {
        when(itineraryService.findById(99L)).thenThrow(new ItineraryNotFoundException(99L));

        mockMvc.perform(get("/api/itineraries/99").with(reader()).header("X-Correlation-ID", "abc-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/itineraries/99"))
                .andExpect(jsonPath("$.correlationId").value("abc-123"));
    }

    @Test
    void post_valido_devuelve201() throws Exception {
        when(itineraryService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/itineraries").with(writer()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.travelDate").isString());
    }

    @Test
    void post_duracionNoPositiva_devuelve400SinLlamarAlService() throws Exception {
        String body = VALID_BODY.replace("\"durationMinutes\":60", "\"durationMinutes\":0");

        mockMvc.perform(post("/api/itineraries").with(writer()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verifyNoInteractions(itineraryService);
    }

    @Test
    void post_aeropuertoInexistente_devuelve400() throws Exception {
        when(itineraryService.create(any())).thenThrow(new InvalidAirportException("XXX"));

        mockMvc.perform(post("/api/itineraries").with(writer()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_AIRPORT"));
    }

    @Test
    void post_airportServiceCaido_devuelve503() throws Exception {
        when(itineraryService.create(any())).thenThrow(new ExternalServiceException("down", null));

        mockMvc.perform(post("/api/itineraries").with(writer()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void put_valido_devuelve200() throws Exception {
        when(itineraryService.update(eq(1L), any())).thenReturn(sample());

        mockMvc.perform(put("/api/itineraries/1").with(writer()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_existente_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/itineraries/1").with(writer()))
                .andExpect(status().isNoContent());
        verify(itineraryService).delete(1L);
    }

    @Test
    void errorInesperado_devuelve500SinFiltrarDetalles() throws Exception {
        when(itineraryService.findAll()).thenThrow(new IllegalStateException("detalle interno"));

        mockMvc.perform(get("/api/itineraries").with(reader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }

    @Test
    void post_jsonMalformado_devuelve400() throws Exception {
        mockMvc.perform(post("/api/itineraries").with(writer()).contentType(MediaType.APPLICATION_JSON).content("{malformado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void getById_noNumerico_devuelve400() throws Exception {
        mockMvc.perform(get("/api/itineraries/abc").with(reader()))
                .andExpect(status().isBadRequest());
    }

    // ---- API versioning + paginacion (paso 31) ----

    @Test
    void getAll_sinHeaderDeVersion_atiendeV1ConArreglo() throws Exception {
        when(itineraryService.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/itineraries").with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(itineraryService, never()).findPage(anyInt(), anyInt());
    }

    @Test
    void getAll_v2_devuelvePagina() throws Exception {
        when(itineraryService.findPage(1, 5)).thenReturn(new PageResponse<>(List.of(sample()), 1, 5, 6, 2));

        mockMvc.perform(get("/api/itineraries").with(reader()).header("API-Version", "2").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getAll_v2_tamanioFueraDeRango_devuelve400() throws Exception {
        mockMvc.perform(get("/api/itineraries").with(reader()).header("API-Version", "2").param("size", "101"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(itineraryService);
    }

    @Test
    void versionNoSoportada_devuelve400() throws Exception {
        mockMvc.perform(get("/api/itineraries").with(reader()).header("API-Version", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("3")));
    }

    @Test
    void endpointSinVersion_funcionaTambienEnV2() throws Exception {
        when(itineraryService.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/itineraries").with(writer()).header("API-Version", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    // ---- Seguridad S2S (paso 31) ----

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/itineraries"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void escrituraSinBearerNiTokenCsrf_laRechazaCsrf() throws Exception {
        // CSRF sigue activo (Sonar S4502): sin Bearer (p. ej. un navegador con cookies) el CsrfFilter,
        // que corre antes de la autenticacion, rechaza con 403 en vez del 401 por falta de token.
        // (jwt() de spring-security-test exime de CSRF como una peticion Bearer real, por eso aqui no se usa.)
        mockMvc.perform(post("/api/itineraries").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(itineraryService);
    }

    @Test
    void escribirConScopeDeLectura_devuelve403() throws Exception {
        mockMvc.perform(post("/api/itineraries").with(reader()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(itineraryService);
    }
}
