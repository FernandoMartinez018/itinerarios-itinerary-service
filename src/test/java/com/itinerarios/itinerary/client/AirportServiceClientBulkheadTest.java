package com.itinerarios.itinerary.client;

import com.itinerarios.itinerary.exception.AirportServiceSaturatedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Prueba el cableado real del Bulkhead (paso 31): anotaciones + application.yml + fallback,
 * con un contexto Spring minimo (AOP + Resilience4j, sin BD ni broker). Se reduce el limite a 2
 * llamadas concurrentes y sin espera para que la prueba sea deterministica.
 */
@SpringBootTest(classes = AirportServiceClientBulkheadTest.TestConfig.class, properties = {
        "resilience4j.bulkhead.instances.airportService.max-concurrent-calls=2",
        "resilience4j.bulkhead.instances.airportService.max-wait-duration=0ms"
})
class AirportServiceClientBulkheadTest {

    static final CountDownLatch entered = new CountDownLatch(2);
    static final CountDownLatch release = new CountDownLatch(1);
    static final RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
    static final AtomicInteger httpCalls = new AtomicInteger();

    @Configuration
    @ImportAutoConfiguration({AopAutoConfiguration.class, BulkheadAutoConfiguration.class,
            CircuitBreakerAutoConfiguration.class, RetryAutoConfiguration.class})
    static class TestConfig {
        @Bean
        AirportServiceClient airportServiceClient() {
            return new AirportServiceClient(restClient);
        }
    }

    @Autowired
    private AirportServiceClient client;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @AfterEach
    void releaseBlockedCalls() {
        release.countDown();
    }

    @Test
    void bulkheadLleno_fallaRapidoSinReintentarNiAbrirElCircuito() throws Exception {
        // Airport Service "lento": cada llamada queda bloqueada hasta liberar el latch.
        when(restClient.get().uri(anyString(), any(Object[].class)).retrieve().body(AirportSummary.class))
                .thenAnswer(inv -> {
                    httpCalls.incrementAndGet();
                    entered.countDown();
                    release.await(10, TimeUnit.SECONDS);
                    return new AirportSummary(1L, "BOG", "El Dorado", true);
                });

        CompletableFuture<AirportSummary> first = CompletableFuture.supplyAsync(() -> client.validateAirport("BOG"));
        CompletableFuture<AirportSummary> second = CompletableFuture.supplyAsync(() -> client.validateAirport("MDE"));
        assertThat(entered.await(5, TimeUnit.SECONDS)).as("las 2 plazas del bulkhead ocupadas").isTrue();

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.validateAirport("CLO"))
                .isInstanceOf(AirportServiceSaturatedException.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).as("falla rapido, sin backoff de Retry").isLessThan(250);
        assertThat(circuitBreakerRegistry.circuitBreaker("airportService").getState())
                .as("la saturacion local no cuenta como fallo de Airport").isEqualTo(CLOSED);

        release.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS).iataCode()).isEqualTo("BOG");
        assertThat(second.get(5, TimeUnit.SECONDS).iataCode()).isEqualTo("BOG");
        // Solo las 2 llamadas admitidas llegaron al cliente HTTP: la rechazada no se reintento.
        assertThat(httpCalls.get()).isEqualTo(2);
    }
}
