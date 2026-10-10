package com.itinerarios.itinerary.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient airportServiceRestClient(
            @Value("${integrations.airport-service.base-url}") String baseUrl,
            @Value("${integrations.airport-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${integrations.airport-service.read-timeout-ms:3000}") int readTimeoutMs) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                // Propaga el Correlation ID de la petición entrante hacia Airport Service
                // (sección 63: cada request debe poder rastrearse end-to-end).
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (correlationId != null) {
                        request.getHeaders().add(CorrelationIdFilter.HEADER_NAME, correlationId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
