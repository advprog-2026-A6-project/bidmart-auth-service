package id.ac.ui.cs.advprog.bidmartauthservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartauthservice.config.RabbitMqConfig;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEvent;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.AuthEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
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
        return event;
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gagal menyimpan payload event autentikasi", exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchAfterCommit(AuthEvent event) {
        dispatchPendingEvent(event.getId());
    }

    @Scheduled(fixedDelayString = "${auth.events.retry-delay-ms:10000}")
    public void retryUnpublishedEvents() {
        authEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()
                .forEach(event -> dispatchPendingEvent(event.getId()));
    }

    @Transactional
    public void dispatchPendingEvent(Long eventId) {
        authEventRepository.findByIdAndPublishedAtIsNull(eventId)
                .ifPresent(this::publishToBroker);
    }

    private void publishToBroker(AuthEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_NAME,
                    RabbitMqConfig.ROUTING_KEY_PREFIX + event.getEventType().name().toLowerCase(),
                    event.getPayload()
            );
            event.setPublishedAt(LocalDateTime.now());
            authEventRepository.save(event);
        } catch (Exception exception) {
            log.warn("Gagal mengirim event {} ke RabbitMQ. Akan dicoba lagi.", event.getId(), exception);
        }
    }
}
