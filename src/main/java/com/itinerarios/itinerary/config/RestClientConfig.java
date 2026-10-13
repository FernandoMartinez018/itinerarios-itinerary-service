package com.itinerarios.itinerary.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    static final String AIRPORT_CLIENT_REGISTRATION = "itinerary-service";
    private static final AnonymousAuthenticationToken SERVICE_PRINCIPAL = new AnonymousAuthenticationToken(
            "itinerary-service", "itinerary-service", AuthorityUtils.createAuthorityList("ROLE_SERVICE"));

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
                                                                 OAuth2AuthorizedClientService clients) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, clients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    public RestClient airportServiceRestClient(
            @Value("${integrations.airport-service.base-url}") String baseUrl,
            @Value("${integrations.airport-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${integrations.airport-service.read-timeout-ms:3000}") int readTimeoutMs,
            OAuth2AuthorizedClientManager authorizedClientManager) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        // Token de servicio (Client Credentials, scope airport.read) en cada llamada a Airport Service
        // (paso 31). Se cachea hasta que expira; el principal es fijo porque el token es del servicio,
        // no del usuario que origino la peticion.
        OAuth2ClientHttpRequestInterceptor oauth2 = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2.setClientRegistrationIdResolver(request -> AIRPORT_CLIENT_REGISTRATION);
        oauth2.setPrincipalResolver(request -> SERVICE_PRINCIPAL);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(oauth2)
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
