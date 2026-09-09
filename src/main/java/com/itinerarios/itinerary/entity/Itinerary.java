package com.itinerarios.itinerary.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Agregado principal del Itinerary Context.
 * No contiene una entidad Airport completa (sección 11): solo referencias
 * (código IATA) validadas vía HTTP contra Airport Service. Esto evita
 * compartir entidades/tablas entre microservicios (sección 102).
 */
@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false, length = 150)
    private String userName;

    /** Código IATA del aeropuerto de salida, validado contra Airport Service. */
    @Column(name = "departure_airport_id", nullable = false, length = 50)
    private String departureAirportId;

    /** Código IATA del aeropuerto de llegada, validado contra Airport Service. */
    @Column(name = "arrival_airport_id", nullable = false, length = 50)
    private String arrivalAirportId;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Itinerary() {
        // requerido por JPA
    }

    public Itinerary(String userName, String departureAirportId, String arrivalAirportId,
                      LocalDate travelDate, Integer durationMinutes) {
        this.userName = userName;
        this.departureAirportId = departureAirportId;
        this.arrivalAirportId = arrivalAirportId;
        this.travelDate = travelDate;
        this.durationMinutes = durationMinutes;
    }

    public void update(String userName, String departureAirportId, String arrivalAirportId,
                        LocalDate travelDate, Integer durationMinutes) {
        this.userName = userName;
        this.departureAirportId = departureAirportId;
        this.arrivalAirportId = arrivalAirportId;
        this.travelDate = travelDate;
        this.durationMinutes = durationMinutes;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ---- getters ----

    public Long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getDepartureAirportId() {
        return departureAirportId;
    }

    public String getArrivalAirportId() {
        return arrivalAirportId;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
