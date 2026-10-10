# itinerarios-itinerary-service

Microservicio del Itinerary Context — Sistema de Itinerarios Personales.

## Responsabilidad

- CRUD completo de itinerarios personales.
- Validación de aeropuertos (`departureAirportId` / `arrivalAirportId`, códigos IATA)
  mediante HTTP contra `itinerarios-airport-service`. Nunca accede directamente a `airport_db`
  (sección 102 de la especificación maestra).
- Persistencia en `itinerary_db` (PostgreSQL propio).

## Stack

- Java 21
- Spring Boot 4.1.1
- PostgreSQL + Flyway
- springdoc-openapi (Swagger UI en `/swagger-ui.html`)

## Ejecutar localmente

Requiere PostgreSQL y `itinerarios-airport-service` corriendo.

```bash
export DB_HOST=localhost
export DB_PORT=5434
export DB_NAME=itinerary_db
export DB_USERNAME=itinerary_user
export DB_PASSWORD=changeme
export AIRPORT_SERVICE_URL=http://localhost:8081

mvn spring-boot:run
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/itineraries` | Lista todos los itinerarios |
| GET | `/api/itineraries/{id}` | Itinerario por id |
| POST | `/api/itineraries` | Crea un itinerario (valida aeropuertos) |
| PUT | `/api/itineraries/{id}` | Actualiza un itinerario (valida aeropuertos) |
| DELETE | `/api/itineraries/{id}` | Elimina un itinerario |

## Errores relevantes

| Situación | HTTP |
|---|---|
| Itinerario no encontrado | 404 |
| Código de aeropuerto inexistente | 400 |
| Airport Service no disponible | 503 |
| `durationMinutes <= 0` u otros campos inválidos | 400 |

## Resiliencia (Nivel 2, Fase 10)

`AirportServiceClient.validateAirport()` está protegido con `@Retry` +
`@CircuitBreaker` (Resilience4j), con la misma configuración de ventana/umbral
que Airport Service (ver `application.yml`).

Decisión importante: `InvalidAirportException` (Airport Service responde 404,
"el aeropuerto no existe", e Itinerary Service lo traduce a HTTP 400) está en la lista de excepciones **ignoradas** tanto por Retry como por
Circuit Breaker — un 404 es una respuesta de negocio válida, no una falla
transitoria del servicio. Reintentarlo no cambiaría el resultado, y contarlo
como "fallo" haría que crear itinerarios con códigos IATA inválidos abriera el
circuito innecesariamente, bloqueando también las validaciones legítimas.

## Observabilidad (Nivel 2, Fase 11)

Igual que Airport Service: agente Java de OpenTelemetry (auto-instrumentación, sin
cambios de código) exportando trazas a Jaeger vía el `otel-collector`, y métricas
Micrometer/Prometheus en `/actuator/prometheus`. Ver el README de
`itinerarios-airport-service` para el detalle de por qué conviven el
`correlationId` propio y el `trace_id` de OpenTelemetry sin unificarse.

## Transactional Outbox (Nivel 3, Fase 12)

`ItineraryService.create()` ya no publica a RabbitMQ directamente. En su lugar,
escribe una fila en `outbox_events` (estado `PENDING`) en la **misma transacción**
de PostgreSQL que el itinerario — ver `ADR-008` en `itinerarios-docs` para el
detalle completo del Double Write Problem que esto resuelve.

`OutboxPublisher` (`@Scheduled`, cada 2s por defecto, configurable vía
`OUTBOX_POLL_INTERVAL_MS`) lee las filas pendientes/fallidas y las publica a
RabbitMQ de forma asíncrona, marcándolas `PUBLISHED` o incrementando su
`retry_count` (máximo 5 intentos) si vuelve a fallar.

## Pendiente

Ninguno para Nivel 3 "prioridad muy alta" (sección 108) en este servicio. Sigue
CI/CD completo (Fase 13) y, según tiempo disponible, componentes adicionales de
Nivel 3 (gRPC, mTLS, Kubernetes, Pact, etc.).
