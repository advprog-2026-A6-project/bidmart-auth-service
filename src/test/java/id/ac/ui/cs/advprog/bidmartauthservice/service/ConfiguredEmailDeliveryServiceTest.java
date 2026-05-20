package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.config.AuthEmailProperties;
import id.ac.ui.cs.advprog.bidmartauthservice.config.AuthVerificationProperties;
import id.ac.ui.cs.advprog.bidmartauthservice.model.EmailDeliveryMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfiguredEmailDeliveryServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private AuthEmailProperties authEmailProperties;
    private AuthVerificationProperties authVerificationProperties;
    private ConfiguredEmailDeliveryService configuredEmailDeliveryService;

    @BeforeEach
    void setUp() {
        authEmailProperties = new AuthEmailProperties();
        authVerificationProperties = new AuthVerificationProperties();
        configuredEmailDeliveryService = new ConfiguredEmailDeliveryService(
                javaMailSender,
                authEmailProperties,
                authVerificationProperties
        );
    }

    @Test
    void sendVerificationEmail_UsesSmtpWhenConfigured() {
        authEmailProperties.setDeliveryMode(EmailDeliveryMode.SMTP);
        authEmailProperties.setFromAddress("no-reply@bidmart.test");

        configuredEmailDeliveryService.sendVerificationEmail("user@mail.com", "verify-token");

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendTwoFactorCode_UsesLoggingModeWithoutSendingMail() {
        authEmailProperties.setDeliveryMode(EmailDeliveryMode.LOGGING);

        configuredEmailDeliveryService.sendTwoFactorCode("user@mail.com", "123456");

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }
}
