package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.config.AuthSessionProperties;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.*;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Spy
    private AuthSessionProperties authSessionProperties = new AuthSessionProperties();

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Aaron Nathanael");
        registerRequest.setEmail("aaron@test.com");
        registerRequest.setPassword("rahasia123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("aaron@test.com");
        loginRequest.setPassword("rahasia123");

        user = User.builder()
                .id(1L)
                .name("Aaron Nathanael")
                .email("aaron@test.com")
                .password("encoded_password")
                .emailVerified(true)
                .isTwoFactorEnabled(false)
                .twoFactorMethod(TwoFactorMethod.NONE)
                .roles(new HashSet<>())
                .build();

        role = Role.builder().id(1L).name("BUYER").build();
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = authService.register(registerRequest);

        assertNotNull(savedUser);
        assertEquals("aaron@test.com", savedUser.getEmail());
        assertFalse(savedUser.isEmailVerified());
        verify(userRepository, times(1)).save(any(User.class));
        verify(verificationTokenService, times(1)).createEmailVerification(any(User.class));
    }

    @Test
    void testRegisterEmailAlreadyExists() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email sudah terdaftar!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterFailsWhenDefaultRoleMissing() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(registerRequest));

        assertEquals("Role default BUYER tidak ditemukan. Pastikan DataSeeder berjalan.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testVerifyEmailSuccess() {
        VerificationToken token = VerificationToken.builder()
                .token("verify-token")
                .user(user)
                .purpose(VerificationPurpose.EMAIL_VERIFICATION)
                .build();
        user.setEmailVerified(false);

        when(verificationTokenService.getValidEmailVerificationToken("verify-token")).thenReturn(token);

        authService.verifyEmail("verify-token");

        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
        verify(verificationTokenService).markConsumed(token);
    }

    @Test
    void testResendVerificationEmailFailsWhenAlreadyVerified() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authService.resendVerificationEmail(user.getEmail()));

        assertEquals("Email sudah diverifikasi", exception.getMessage());
        verify(verificationTokenService, never()).createEmailVerification(any(User.class));
    }

    @Test
    void testResendVerificationEmailSuccess() {
        user.setEmailVerified(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        authService.resendVerificationEmail(user.getEmail());

        verify(verificationTokenService).createEmailVerification(user);
    }

    @Test
    void testResendVerificationEmailFailsWhenUserMissing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resendVerificationEmail("missing@test.com")
        );

        assertEquals("User tidak ditemukan", exception.getMessage());
    }

    @Test
    void testLoginSuccessNo2fa() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(eq(user), anyString())).thenReturn("dummy_access_token");
        when(jwtService.generateRefreshToken(eq(user), anyString())).thenReturn("dummy_refresh_token");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Device-Test");
        when(userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtAsc(user.getId())).thenReturn(new ArrayList<>());

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertFalse(response.isMfaRequired());
        assertEquals("dummy_access_token", response.getAccessToken());
        assertEquals("dummy_refresh_token", response.getRefreshToken());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userSessionRepository, times(1)).save(any(UserSession.class));
    }

    @Test
    void testLoginSuccessWithMaxDevices() {
        authSessionProperties.setMaxConcurrentSessions(3);
        authSessionProperties.setOverflowPolicy(SessionOverflowPolicy.REVOKE_OLDEST);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(eq(user), anyString())).thenReturn("dummy_access_token");
        when(jwtService.generateRefreshToken(eq(user), anyString())).thenReturn("dummy_refresh_token");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Device-Test");

        List<UserSession> activeSessions = new ArrayList<>();
        UserSession oldSession = UserSession.builder()
                .id(1L)
                .createdAt(LocalDateTime.now().minusDays(3))
                .isActive(true)
                .build();
        activeSessions.add(oldSession);
        activeSessions.add(UserSession.builder().id(2L).createdAt(LocalDateTime.now().minusDays(2)).isActive(true).build());
        activeSessions.add(UserSession.builder().id(3L).createdAt(LocalDateTime.now().minusDays(1)).isActive(true).build());

        when(userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtAsc(user.getId())).thenReturn(activeSessions);

        authService.login(loginRequest);

        assertFalse(oldSession.isActive());
        verify(userSessionRepository, times(2)).save(any(UserSession.class));
    }

    @Test
    void testLoginRejectedWhenMaxDevicesReachedAndPolicyRejectNew() {
        authSessionProperties.setMaxConcurrentSessions(1);
        authSessionProperties.setOverflowPolicy(SessionOverflowPolicy.REJECT_NEW);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Device-Test");
        when(userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of(UserSession.builder().id(1L).isActive(true).build()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> authService.login(loginRequest));

        assertEquals("Batas sesi aktif tercapai. Silakan logout dari perangkat lain terlebih dahulu.", exception.getMessage());
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void testLoginSuccessWith2fa() {
        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod(TwoFactorMethod.TOTP);
        VerificationToken challenge = VerificationToken.builder()
                .token("challenge-token")
                .method(VerificationMethod.TOTP)
                .user(user)
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Device-Test");
        when(verificationTokenService.createLoginChallenge(user, "Device-Test")).thenReturn(challenge);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertTrue(response.isMfaRequired());
        assertEquals("", response.getAccessToken());
        assertEquals("", response.getRefreshToken());
        assertEquals("challenge-token", response.getMfaChallengeToken());
        assertEquals("TOTP", response.getTwoFactorMethod());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLoginRejectedWhenEmailNotVerified() {
        user.setEmailVerified(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> authService.login(loginRequest));

        assertEquals("Email belum diverifikasi", exception.getMessage());
    }

    @Test
    void testLoginRejectedWhenUserInactive() {
        user.setActive(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));

        DisabledException exception = assertThrows(DisabledException.class, () -> authService.login(loginRequest));

        assertEquals("Akun pengguna telah dinonaktifkan", exception.getMessage());
    }

    @Test
    void testLoginUserNotFound() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(loginRequest));

        assertEquals("Email atau kata sandi salah!", exception.getMessage());
    }

    @Test
    void testVerify2faSuccess() {
        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod(TwoFactorMethod.TOTP);
        user.setTwoFactorSecret("SECRET_KEY");

        VerificationToken challenge = VerificationToken.builder()
                .token("challenge-token")
                .user(user)
                .method(VerificationMethod.TOTP)
                .deviceId("Device-Test")
                .build();

        when(verificationTokenService.getValidLoginChallenge("challenge-token")).thenReturn(challenge);
        when(twoFactorAuthService.isOtpValid("SECRET_KEY", "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(eq(user), anyString())).thenReturn("dummy_access_token");
        when(jwtService.generateRefreshToken(eq(user), anyString())).thenReturn("dummy_refresh_token");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Device-Test");
        when(userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtAsc(user.getId())).thenReturn(new ArrayList<>());

        AuthResponse response = authService.verify2fa("challenge-token", "123456");

        assertNotNull(response);
        assertFalse(response.isMfaRequired());
        assertEquals("dummy_access_token", response.getAccessToken());
        assertEquals("dummy_refresh_token", response.getRefreshToken());
        verify(userSessionRepository, times(1)).save(any(UserSession.class));
        verify(verificationTokenService).markConsumed(challenge);
    }

    @Test
    void testVerify2faFailed() {
        user.setTwoFactorSecret("SECRET_KEY");
        VerificationToken challenge = VerificationToken.builder()
                .token("challenge-token")
                .user(user)
                .method(VerificationMethod.TOTP)
                .deviceId("Device-Test")
                .build();

        when(verificationTokenService.getValidLoginChallenge("challenge-token")).thenReturn(challenge);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Device-Test");
        when(twoFactorAuthService.isOtpValid("SECRET_KEY", "000000")).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authService.verify2fa("challenge-token", "000000"));

        assertEquals("Kode OTP salah atau sudah kedaluwarsa", exception.getMessage());
    }

    @Test
    void testVerify2faSuccessWithEmailMethod() {
        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod(TwoFactorMethod.EMAIL);

        VerificationToken challenge = VerificationToken.builder()
                .token("challenge-token")
                .user(user)
                .method(VerificationMethod.EMAIL)
                .code("654321")
                .deviceId("Device-Test")
                .build();

        when(verificationTokenService.getValidLoginChallenge("challenge-token")).thenReturn(challenge);
        when(jwtService.generateAccessToken(eq(user), anyString())).thenReturn("dummy_access_token");
        when(jwtService.generateRefreshToken(eq(user), anyString())).thenReturn("dummy_refresh_token");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Device-Test");
        when(userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtAsc(user.getId())).thenReturn(new ArrayList<>());

        AuthResponse response = authService.verify2fa("challenge-token", "654321");

        assertEquals("dummy_access_token", response.getAccessToken());
        assertEquals("EMAIL", response.getTwoFactorMethod());
        verify(twoFactorAuthService, never()).isOtpValid(anyString(), anyString());
        verify(verificationTokenService).markConsumed(challenge);
    }

    @Test
    void testVerify2faFailsWhenDeviceMismatch() {
        VerificationToken challenge = VerificationToken.builder()
                .token("challenge-token")
                .user(user)
                .method(VerificationMethod.EMAIL)
                .code("654321")
                .deviceId("Device-Test")
                .build();

        when(verificationTokenService.getValidLoginChallenge("challenge-token")).thenReturn(challenge);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Other-Device");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.verify2fa("challenge-token", "654321")
        );

        assertEquals("Challenge 2FA tidak berlaku untuk perangkat ini", exception.getMessage());
    }

    @Test
    void testRefreshSuccess() {
        UserSession session = UserSession.builder()
                .id(1L)
                .sessionTokenId("session-123")
                .user(user)
                .refreshToken("old-refresh")
                .deviceId("Device-Test")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();

        when(jwtService.isRefreshToken("old-refresh")).thenReturn(true);
        when(jwtService.extractUsername("old-refresh")).thenReturn(user.getEmail());
        when(jwtService.extractSessionTokenId("old-refresh")).thenReturn("session-123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("old-refresh", user)).thenReturn(true);
        when(userSessionRepository.findBySessionTokenId("session-123")).thenReturn(Optional.of(session));
        when(jwtService.generateAccessToken(user, "session-123")).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user, "session-123")).thenReturn("new-refresh");

        AuthResponse response = authService.refresh("old-refresh");

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(userSessionRepository).save(session);
    }

    @Test
    void testRefreshFailsWhenTokenBlank() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.refresh(" "));
        assertEquals("Refresh token wajib diisi", exception.getMessage());
    }

    @Test
    void testRefreshFailsWhenTokenTypeInvalid() {
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.refresh("access-token"));

        assertEquals("Token yang diberikan bukan refresh token", exception.getMessage());
    }

    @Test
    void testRefreshFailsWhenUserMissing() {
        when(jwtService.isRefreshToken("refresh")).thenReturn(true);
        when(jwtService.extractUsername("refresh")).thenReturn("missing@test.com");
        when(jwtService.extractSessionTokenId("refresh")).thenReturn("session-123");
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.refresh("refresh"));

        assertEquals("User tidak ditemukan", exception.getMessage());
    }

    @Test
    void testRefreshFailsWhenTokenInvalid() {
        when(jwtService.isRefreshToken("refresh")).thenReturn(true);
        when(jwtService.extractUsername("refresh")).thenReturn(user.getEmail());
        when(jwtService.extractSessionTokenId("refresh")).thenReturn("session-123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh", user)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.refresh("refresh"));

        assertEquals("Refresh token tidak valid", exception.getMessage());
    }

    @Test
    void testRefreshFailsWhenSessionMissing() {
        when(jwtService.isRefreshToken("refresh")).thenReturn(true);
        when(jwtService.extractUsername("refresh")).thenReturn(user.getEmail());
        when(jwtService.extractSessionTokenId("refresh")).thenReturn("session-123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh", user)).thenReturn(true);
        when(userSessionRepository.findBySessionTokenId("session-123")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.refresh("refresh"));

        assertEquals("Sesi tidak ditemukan", exception.getMessage());
    }

    @Test
    void testRefreshFailsWhenSessionRevoked() {
        UserSession session = UserSession.builder()
                .sessionTokenId("session-123")
                .refreshToken("refresh")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .isActive(false)
                .build();

        when(jwtService.isRefreshToken("refresh")).thenReturn(true);
        when(jwtService.extractUsername("refresh")).thenReturn(user.getEmail());
        when(jwtService.extractSessionTokenId("refresh")).thenReturn("session-123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh", user)).thenReturn(true);
        when(userSessionRepository.findBySessionTokenId("session-123")).thenReturn(Optional.of(session));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> authService.refresh("refresh"));

        assertEquals("Sesi sudah dicabut", exception.getMessage());
    }

    @Test
    void testRefreshFailsWhenSessionExpired() {
        UserSession session = UserSession.builder()
                .sessionTokenId("session-123")
                .refreshToken("refresh")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .isActive(true)
                .build();

        when(jwtService.isRefreshToken("refresh")).thenReturn(true);
        when(jwtService.extractUsername("refresh")).thenReturn(user.getEmail());
        when(jwtService.extractSessionTokenId("refresh")).thenReturn("session-123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh", user)).thenReturn(true);
        when(userSessionRepository.findBySessionTokenId("session-123")).thenReturn(Optional.of(session));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> authService.refresh("refresh"));

        assertEquals("Refresh token sudah kedaluwarsa", exception.getMessage());
        assertFalse(session.isActive());
        verify(userSessionRepository).save(session);
    }

    @Test
    void testLogoutRevokesSession() {
        UserSession session = UserSession.builder()
                .sessionTokenId("session-123")
                .isActive(true)
                .build();

        when(jwtService.extractSessionTokenId("access-token")).thenReturn("session-123");
        when(userSessionRepository.findBySessionTokenId("session-123")).thenReturn(Optional.of(session));

        authService.logout("Bearer access-token");

        assertFalse(session.isActive());
        verify(userSessionRepository).save(session);
    }

    @Test
    void testLogoutFailsWhenAuthorizationHeaderInvalid() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.logout("Invalid"));
        assertEquals("Authorization header tidak valid", exception.getMessage());
    }

    @Test
    void testLogoutFailsWhenSessionMissing() {
        when(jwtService.extractSessionTokenId("access-token")).thenReturn("session-404");
        when(userSessionRepository.findBySessionTokenId("session-404")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.logout("Bearer access-token")
        );

        assertEquals("Sesi tidak ditemukan", exception.getMessage());
    }
}
