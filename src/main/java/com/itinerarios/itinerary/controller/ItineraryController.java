package com.itinerarios.itinerary.controller;

import com.itinerarios.itinerary.dto.ItineraryDto;
import com.itinerarios.itinerary.dto.ItineraryRequest;
import com.itinerarios.itinerary.service.ItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itineraries")
@Tag(name = "Itineraries", description = "CRUD de itinerarios personales de viaje")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los itinerarios")
    public ResponseEntity<List<ItineraryDto>> findAll() {
        return ResponseEntity.ok(itineraryService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un itinerario por id")
    public ResponseEntity<ItineraryDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(itineraryService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un itinerario, validando aeropuertos contra Airport Service")
    public ResponseEntity<ItineraryDto> create(@Valid @RequestBody ItineraryRequest request) {
        ItineraryDto created = itineraryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un itinerario existente")
    public ResponseEntity<ItineraryDto> update(@PathVariable Long id, @Valid @RequestBody ItineraryRequest request) {
        return ResponseEntity.ok(itineraryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un itinerario")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itineraryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
