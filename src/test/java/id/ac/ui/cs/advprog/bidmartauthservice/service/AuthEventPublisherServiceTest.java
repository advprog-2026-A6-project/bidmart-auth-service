package id.ac.ui.cs.advprog.bidmartauthservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEvent;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.AuthEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private ObjectMapper objectMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AuthEventPublisherService authEventPublisherService;

    @Test
    void publishPersistsEventPublishesApplicationEventAndSendsRabbitMessage() throws Exception {
        when(authEventRepository.save(any(AuthEvent.class))).thenAnswer(invocation -> {
            AuthEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(1L);
            }
            return event;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"userId\":1}");

        AuthEvent event = authEventPublisherService.publish(
                AuthEventType.ACCOUNT_DISABLED,
                "USER",
                "1",
                Map.of("userId", 1)
        );

        assertThat(event.getPayload()).isEqualTo("{\"userId\":1}");
        assertThat(event.getPublishedAt()).isNotNull();
        verify(applicationEventPublisher).publishEvent(event);
        verify(rabbitTemplate).convertAndSend("bidmart.auth.exchange", "auth.event.account_disabled", "{\"userId\":1}");
        verify(authEventRepository, times(2)).save(any(AuthEvent.class));
    }

    @Test
    void publishStillMarksEventWhenRabbitFails() throws Exception {
        when(authEventRepository.save(any(AuthEvent.class))).thenAnswer(invocation -> {
            AuthEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(1L);
            }
            return event;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"role\":\"ADMIN\"}");
        doThrow(new RuntimeException("rabbit down"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(String.class));

        AuthEvent event = authEventPublisherService.publish(
                AuthEventType.ROLE_CREATED,
                "ROLE",
                "ADMIN",
                Map.of("role", "ADMIN")
        );

        assertThat(event.getPublishedAt()).isNotNull();
        verify(applicationEventPublisher).publishEvent(event);
        verify(authEventRepository, times(2)).save(any(AuthEvent.class));
    }

    @Test
    void publishThrowsWhenPayloadSerializationFails() throws Exception {
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("boom") {});

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authEventPublisherService.publish(
                        AuthEventType.PERMISSION_CREATED,
                        "PERMISSION",
                        "bid:place",
                        Map.of("permission", "bid:place")
                )
        );

        assertThat(exception.getMessage()).isEqualTo("Gagal menyimpan payload event autentikasi");
        verify(authEventRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher, rabbitTemplate);
    }
}
