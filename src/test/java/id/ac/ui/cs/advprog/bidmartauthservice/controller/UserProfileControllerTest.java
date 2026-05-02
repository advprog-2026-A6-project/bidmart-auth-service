package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.service.JwtService;
import id.ac.ui.cs.advprog.bidmartauthservice.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = UserProfileController.class)
@AutoConfigureMockMvc
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ProfileResponseDto dummyResponse;

    @BeforeEach
    void setUp() {
        dummyResponse = ProfileResponseDto.builder()
                .id(1L)
                .email("test@example.com")
                .name("Tester")
                .phoneNumber("08123456789")
                .address("Jl. Testing No. 1")
                .bio("I am a tester")
                .profilePictureUrl("https://example.com/pic.jpg")
                .isTwoFactorEnabled(false)
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void testGetProfile() throws Exception {
        when(userProfileService.getProfile(anyString())).thenReturn(dummyResponse);

        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Tester"))
                .andExpect(jsonPath("$.phoneNumber").value("08123456789"))
                .andExpect(jsonPath("$.twoFactorEnabled").value(false));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "SELLER")
    void testUpdateProfile() throws Exception {
        ProfileUpdateDto updateDto = ProfileUpdateDto.builder()
                .name("Tester Updated")
                .phoneNumber("08999999999")
                .address("Jl. Baru No. 2")
                .bio("Updated bio")
                .profilePictureUrl("https://example.com/newpic.jpg")
                .build();

        when(userProfileService.updateProfile(anyString(), any(ProfileUpdateDto.class))).thenReturn(dummyResponse);

        mockMvc.perform(put("/api/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Tester"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "ADMIN")
    void testGenerate2faQrCode() throws Exception {
        when(userProfileService.generate2faQrCode(anyString())).thenReturn("data:image/png;base64,dummyqr");

        mockMvc.perform(get("/api/profile/2fa/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCodeUri").value("data:image/png;base64,dummyqr"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void testEnable2faSuccess() throws Exception {
        when(userProfileService.enable2fa(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/profile/2fa/enable")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("2FA berhasil diaktifkan!"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void testEnable2faFailed() throws Exception {
        when(userProfileService.enable2fa(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/profile/2fa/enable")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Kode OTP salah atau kedaluwarsa. Silakan coba lagi."));
    }
}