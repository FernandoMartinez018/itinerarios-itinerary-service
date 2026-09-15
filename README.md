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

## Pendiente (Nivel 2+)

- Publicar `ItineraryCreatedEvent` a RabbitMQ al crear un itinerario (Fase 7).
- Retry / Backoff / Jitter / Circuit Breaker en `AirportServiceClient` (Fase 10).
- Transactional Outbox (Fase 12, Nivel 3).
