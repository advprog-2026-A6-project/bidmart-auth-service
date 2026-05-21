package id.ac.ui.cs.advprog.bidmartauthservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "bidmart.auth.exchange";
    public static final String ROUTING_KEY_PREFIX = "auth.event.";

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }
}
