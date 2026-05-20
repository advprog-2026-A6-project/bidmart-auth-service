package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.config.AuthVerificationProperties;
import id.ac.ui.cs.advprog.bidmartauthservice.model.TwoFactorMethod;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationMethod;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationPurpose;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationToken;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailDeliveryService emailDeliveryService;

    private AuthVerificationProperties authVerificationProperties;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        authVerificationProperties = new AuthVerificationProperties();
        authVerificationProperties.setEmailExpiryHours(24);
        authVerificationProperties.setLoginChallengeExpiryMinutes(5);
        verificationTokenService = new VerificationTokenService(
                verificationTokenRepository,
                emailDeliveryService,
                authVerificationProperties
        );

        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .twoFactorMethod(TwoFactorMethod.NONE)
                .build();
    }

    @Test
    void createEmailVerificationInvalidatesPreviousTokensAndSendsEmail() {
        VerificationToken previous = VerificationToken.builder()
                .id(10L)
                .user(user)
                .token("old-token")
                .purpose(VerificationPurpose.EMAIL_VERIFICATION)
                .method(VerificationMethod.LINK)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(verificationTokenRepository.findByUserIdAndPurposeAndConsumedFalse(1L, VerificationPurpose.EMAIL_VERIFICATION))
                .thenReturn(List.of(previous));
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        verificationTokenService.createEmailVerification(user);

        assertThat(previous.isConsumed()).isTrue();
        verify(verificationTokenRepository).saveAll(List.of(previous));

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());
        VerificationToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getPurpose()).isEqualTo(VerificationPurpose.EMAIL_VERIFICATION);
        assertThat(saved.getMethod()).isEqualTo(VerificationMethod.LINK);
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));

        verify(emailDeliveryService).sendVerificationEmail(eq("user@test.com"), anyString());
    }

    @Test
    void createLoginChallengeWithEmailMethodGeneratesNumericCodeAndSendsEmail() {
        user.setTwoFactorMethod(TwoFactorMethod.EMAIL);
        when(verificationTokenRepository.findByUserIdAndPurposeAndConsumedFalse(1L, VerificationPurpose.LOGIN_2FA))
                .thenReturn(List.of());
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationToken challenge = verificationTokenService.createLoginChallenge(user, "Device-A");

        assertThat(challenge.getPurpose()).isEqualTo(VerificationPurpose.LOGIN_2FA);
        assertThat(challenge.getMethod()).isEqualTo(VerificationMethod.EMAIL);
        assertThat(challenge.getDeviceId()).isEqualTo("Device-A");
        assertThat(challenge.getCode()).hasSize(6).containsOnlyDigits();
        verify(emailDeliveryService).sendTwoFactorCode("user@test.com", challenge.getCode());
        verify(verificationTokenRepository, never()).saveAll(any());
    }

    @Test
    void createLoginChallengeWithTotpDoesNotSendEmailCode() {
        user.setTwoFactorMethod(TwoFactorMethod.TOTP);
        when(verificationTokenRepository.findByUserIdAndPurposeAndConsumedFalse(1L, VerificationPurpose.LOGIN_2FA))
                .thenReturn(List.of());
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationToken challenge = verificationTokenService.createLoginChallenge(user, "Device-B");

        assertThat(challenge.getMethod()).isEqualTo(VerificationMethod.TOTP);
        assertThat(challenge.getCode()).isNull();
        verify(emailDeliveryService, never()).sendTwoFactorCode(anyString(), anyString());
    }

    @Test
    void getValidEmailVerificationTokenThrowsWhenTokenMissing() {
        when(verificationTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> verificationTokenService.getValidEmailVerificationToken("missing")
        );

        assertThat(exception.getMessage()).isEqualTo("Token verifikasi tidak ditemukan");
    }

    @Test
    void getValidEmailVerificationTokenThrowsWhenPurposeMismatch() {
        VerificationToken token = VerificationToken.builder()
                .token("token")
                .user(user)
                .purpose(VerificationPurpose.LOGIN_2FA)
                .method(VerificationMethod.EMAIL)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(verificationTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> verificationTokenService.getValidEmailVerificationToken("token")
        );

        assertThat(exception.getMessage()).isEqualTo("Token verifikasi tidak sesuai");
    }

    @Test
    void getValidLoginChallengeThrowsWhenConsumed() {
        VerificationToken token = VerificationToken.builder()
                .token("token")
                .user(user)
                .purpose(VerificationPurpose.LOGIN_2FA)
                .method(VerificationMethod.TOTP)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .consumed(true)
                .build();
        when(verificationTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> verificationTokenService.getValidLoginChallenge("token")
        );

        assertThat(exception.getMessage()).isEqualTo("Token verifikasi sudah digunakan");
    }

    @Test
    void getValidLoginChallengeThrowsWhenExpired() {
        VerificationToken token = VerificationToken.builder()
                .token("token")
                .user(user)
                .purpose(VerificationPurpose.LOGIN_2FA)
                .method(VerificationMethod.TOTP)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(verificationTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> verificationTokenService.getValidLoginChallenge("token")
        );

        assertThat(exception.getMessage()).isEqualTo("Token verifikasi sudah kedaluwarsa");
    }

    @Test
    void markConsumedUpdatesAndSavesToken() {
        VerificationToken token = VerificationToken.builder()
                .token("token")
                .user(user)
                .purpose(VerificationPurpose.LOGIN_2FA)
                .method(VerificationMethod.TOTP)
                .expiresAt(LocalDateTime.now().plusMinutes(1))
                .consumed(false)
                .build();

        verificationTokenService.markConsumed(token);

        assertThat(token.isConsumed()).isTrue();
        verify(verificationTokenRepository).save(token);
    }
}
