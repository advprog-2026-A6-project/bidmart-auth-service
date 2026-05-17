package id.ac.ui.cs.advprog.bidmartauthservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEvent;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.AuthEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthEventPublisherService {

    private final AuthEventRepository authEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Transactional
    public AuthEvent publish(AuthEventType eventType, String aggregateType, String aggregateId, Map<String, Object> payload) {
        AuthEvent event = authEventRepository.save(AuthEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(writePayload(payload))
                .build());

        applicationEventPublisher.publishEvent(event);

        try {
            rabbitTemplate.convertAndSend("bidmart.auth.exchange", "auth.event." + eventType.name().toLowerCase(), event.getPayload());
        } catch (Exception e) {
            System.err.println("Gagal mengirim event ke RabbitMQ: " + e.getMessage());
        }

        event.setPublishedAt(LocalDateTime.now());
        return authEventRepository.save(event);
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gagal menyimpan payload event autentikasi", exception);
        }
    }
}
