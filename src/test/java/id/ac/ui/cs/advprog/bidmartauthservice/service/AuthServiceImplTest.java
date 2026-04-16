package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

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
                .isTwoFactorEnabled(false)
                .build();
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User savedUser = authService.register(registerRequest);

        assertNotNull(savedUser);
        assertEquals("aaron@test.com", savedUser.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterEmailAlreadyExists() {
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Email sudah terdaftar!", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccessNo2fa() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("dummy_access_token");
        when(jwtService.generateRefreshToken(user)).thenReturn("dummy_refresh_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertFalse(response.isMfaRequired());
        assertEquals("dummy_access_token", response.getAccessToken());
        assertEquals("dummy_refresh_token", response.getRefreshToken());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLoginSuccessWith2fa() {
        user.setTwoFactorEnabled(true);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertTrue(response.isMfaRequired());
        assertEquals("", response.getAccessToken());
        assertEquals("", response.getRefreshToken());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLoginUserNotFound() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Email atau kata sandi salah!", exception.getMessage());
    }

    @Test
    void testVerify2faSuccess() {
        user.setTwoFactorSecret("SECRET_KEY");
        when(userRepository.findByEmail("aaron@test.com")).thenReturn(Optional.of(user));
        when(twoFactorAuthService.isOtpValid("SECRET_KEY", "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("dummy_access_token");
        when(jwtService.generateRefreshToken(user)).thenReturn("dummy_refresh_token");

        AuthResponse response = authService.verify2fa("aaron@test.com", "123456");

        assertNotNull(response);
        assertFalse(response.isMfaRequired());
        assertEquals("dummy_access_token", response.getAccessToken());
        assertEquals("dummy_refresh_token", response.getRefreshToken());
    }

    @Test
    void testVerify2faFailed() {
        user.setTwoFactorSecret("SECRET_KEY");
        when(userRepository.findByEmail("aaron@test.com")).thenReturn(Optional.of(user));
        when(twoFactorAuthService.isOtpValid("SECRET_KEY", "000000")).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authService.verify2fa("aaron@test.com", "000000");
        });

        assertEquals("Kode OTP salah atau sudah kedaluwarsa", exception.getMessage());
    }
}