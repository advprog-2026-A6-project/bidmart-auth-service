package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.*;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.service.AuthService;
import id.ac.ui.cs.advprog.bidmartauthservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Aaron Nathanael");
        registerRequest.setEmail("aaron@test.com");
        registerRequest.setPassword("rahasia123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("aaron@test.com");
        loginRequest.setPassword("rahasia123");
    }

    @Test
    void testRegisterEndpointSuccess() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(null);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registrasi berhasil! Silakan cek email untuk verifikasi akun."));
    }

    @Test
    void testRegisterEndpointEmailExists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email sudah terdaftar!"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email sudah terdaftar!"));
    }

    @Test
    void testVerifyEmailEndpointSuccess() throws Exception {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("verify-token");

        mockMvc.perform(post("/api/auth/verify-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email berhasil diverifikasi."));

        verify(authService).verifyEmail("verify-token");
    }

    @Test
    void testResendVerificationEndpointSuccess() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setEmail("aaron@test.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verifikasi berhasil dikirim ulang."));
    }

    @Test
    void testLoginEndpointSuccess() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .mfaRequired(false)
                .twoFactorMethod("NONE")
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh_token"))
                .andExpect(jsonPath("$.mfaRequired").value(false));
    }

    @Test
    void testVerify2faEndpointSuccess() throws Exception {
        Verify2faRequest request = new Verify2faRequest("challenge-token", "123456");
        AuthResponse response = AuthResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .mfaRequired(false)
                .build();

        when(authService.verify2fa(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/verify-2fa")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh_token"))
                .andExpect(jsonPath("$.mfaRequired").value(false));
    }

    @Test
    void testRefreshEndpointSuccess() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh_token");

        AuthResponse response = AuthResponse.builder()
                .accessToken("new_access_token")
                .refreshToken("new_refresh_token")
                .mfaRequired(false)
                .build();

        when(authService.refresh("refresh_token")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new_access_token"))
                .andExpect(jsonPath("$.refreshToken").value("new_refresh_token"));
    }

    @Test
    @WithMockUser(authorities = "profile:read")
    void testLogoutEndpointSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer access_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout berhasil."));
    }
}
