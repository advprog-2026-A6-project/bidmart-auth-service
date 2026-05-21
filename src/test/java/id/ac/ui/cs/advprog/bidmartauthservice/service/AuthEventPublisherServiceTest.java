package id.ac.ui.cs.advprog.bidmartauthservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartauthservice.config.RabbitMqConfig;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEvent;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.AuthEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthEventPublisherServiceTest {

    @Mock
    private AuthEventRepository authEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private AuthEventPublisherService authEventPublisherService() {
        return new AuthEventPublisherService(
                authEventRepository,
                applicationEventPublisher,
                new ObjectMapper(),
                rabbitTemplate
        );
    }

    @Test
    void publishPersistsEventAndQueuesAfterCommitDispatch() {
        when(authEventRepository.save(any(AuthEvent.class))).thenAnswer(invocation -> {
            AuthEvent event = invocation.getArgument(0);
            event.setId(10L);
            return event;
        });

        AuthEvent result = authEventPublisherService().publish(
                AuthEventType.ACCOUNT_DISABLED,
                "USER",
                "42",
                Map.of("userId", 42L)
        );

        assertNotNull(result.getId());
        assertNull(result.getPublishedAt());
        verify(applicationEventPublisher).publishEvent(result);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void dispatchPendingEventMarksEventPublishedAfterSuccessfulBrokerSend() {
        AuthEvent pendingEvent = AuthEvent.builder()
                .id(22L)
                .eventType(AuthEventType.ACCOUNT_DISABLED)
                .aggregateType("USER")
                .aggregateId("42")
                .payload("{\"userId\":42}")
                .createdAt(LocalDateTime.now())
                .build();

        when(authEventRepository.findByIdAndPublishedAtIsNull(22L)).thenReturn(Optional.of(pendingEvent));
        when(authEventRepository.save(any(AuthEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authEventPublisherService().dispatchPendingEvent(22L);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE_NAME),
                eq("auth.event.account_disabled"),
                eq("{\"userId\":42}")
        );

        ArgumentCaptor<AuthEvent> savedCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(authEventRepository).save(savedCaptor.capture());
        assertNotNull(savedCaptor.getValue().getPublishedAt());
    }

    @Test
    void dispatchPendingEventLeavesEventUnpublishedWhenBrokerSendFails() {
        AuthEvent pendingEvent = AuthEvent.builder()
                .id(23L)
                .eventType(AuthEventType.ACCOUNT_DISABLED)
                .aggregateType("USER")
                .aggregateId("99")
                .payload("{\"userId\":99}")
                .createdAt(LocalDateTime.now())
                .build();

        when(authEventRepository.findByIdAndPublishedAtIsNull(23L)).thenReturn(Optional.of(pendingEvent));
        doThrow(new IllegalStateException("broker down"))
                .when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(String.class));

        authEventPublisherService().dispatchPendingEvent(23L);

        verify(authEventRepository, never()).save(any(AuthEvent.class));
        assertNull(pendingEvent.getPublishedAt());
    }

    @Test
    void retryUnpublishedEventsDispatchesPendingBatch() {
        AuthEvent first = AuthEvent.builder()
                .id(1L)
                .eventType(AuthEventType.ACCOUNT_DISABLED)
                .aggregateType("USER")
                .aggregateId("1")
                .payload("{\"userId\":1}")
                .build();
        AuthEvent second = AuthEvent.builder()
                .id(2L)
                .eventType(AuthEventType.USER_ROLE_CHANGED)
                .aggregateType("USER")
                .aggregateId("2")
                .payload("{\"userId\":2}")
                .build();

        when(authEventRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(first, second));
        when(authEventRepository.findByIdAndPublishedAtIsNull(1L)).thenReturn(Optional.of(first));
        when(authEventRepository.findByIdAndPublishedAtIsNull(2L)).thenReturn(Optional.of(second));
        when(authEventRepository.save(any(AuthEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authEventPublisherService().retryUnpublishedEvents();

        verify(rabbitTemplate, times(2)).convertAndSend(any(String.class), any(String.class), any(String.class));
    }
}
