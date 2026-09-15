package com.itinerarios.itinerary.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Itinerary Service es el publisher: declara el exchange (topic, sección 76)
 * pero NO declara la queue ni el binding — eso es responsabilidad del
 * consumidor (Notification Service, Fase 8), que decide cómo quiere
 * consumir los eventos sin acoplar al publisher a esa decisión.
 */
@Configuration
public class RabbitMQConfig {

    public static final String ITINERARY_EVENTS_EXCHANGE = "itinerary.events";
    public static final String ITINERARY_CREATED_ROUTING_KEY = "itinerary.created";

    @Bean
    public TopicExchange itineraryEventsExchange() {
        return new TopicExchange(ITINERARY_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
