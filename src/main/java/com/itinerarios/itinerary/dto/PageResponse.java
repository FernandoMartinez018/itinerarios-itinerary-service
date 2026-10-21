package com.itinerarios.itinerary.dto;

import java.util.List;

/**
 * Pagina de resultados (API v2, paso 31 - Nivel 3). Contrato propio y estable en vez de
 * serializar el Page de Spring Data, cuyo JSON no es un contrato publico.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
