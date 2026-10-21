package com.itinerarios.itinerary.config;

import com.itinerarios.itinerary.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Seguridad servicio-a-servicio (paso 31, seccion 108): Itinerary Service es resource server
 * OAuth2 (JWT de Keycloak): lectura con itinerary.read y escritura con itinerary.write (hoy solo
 * los tiene el Gateway). Ademas es cliente OAuth2 hacia Airport Service (ver RestClientConfig).
 * Actuator y Swagger quedan abiertos para health checks, Prometheus y la documentacion local.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                // CSRF queda activo: oauth2ResourceServer lo omite en peticiones con Bearer token (el
                // caso normal S2S) y sigue protegiendo cualquier otra (defensa en profundidad, Sonar S4502).
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/itineraries/**").hasAuthority("SCOPE_itinerary.read")
                        .requestMatchers("/api/itineraries/**").hasAuthority("SCOPE_itinerary.write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(objectMapper, req, res, HttpStatus.UNAUTHORIZED, "Missing or invalid bearer token"))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(objectMapper, req, res, HttpStatus.FORBIDDEN, "Insufficient scope")));
        return http.build();
    }

    /** Mismo JSON de error que GlobalExceptionHandler (seccion 64), con el Correlation ID del MDC. */
    private static void writeError(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(OffsetDateTime.now(), status.value(),
                status.name(), message, request.getRequestURI(), MDC.get(CorrelationIdFilter.MDC_KEY)));
    }
}
