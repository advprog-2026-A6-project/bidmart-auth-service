package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.bidmartauthservice.service.JwtService;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;

@WebMvcTest(controllers = UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

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
                .build();
    }

    @Test
    void testGetProfile() throws Exception {
        when(userProfileService.getProfile(anyString())).thenReturn(dummyResponse);

        Principal mockPrincipal = () -> "test@example.com";

        mockMvc.perform(get("/api/profile")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Tester"))
                .andExpect(jsonPath("$.phoneNumber").value("08123456789"));
    }

    @Test
    void testUpdateProfile() throws Exception {
        ProfileUpdateDto updateDto = ProfileUpdateDto.builder()
                .name("Tester Updated")
                .phoneNumber("08999999999")
                .address("Jl. Baru No. 2")
                .bio("Updated bio")
                .profilePictureUrl("https://example.com/newpic.jpg")
                .build();

        when(userProfileService.updateProfile(anyString(), any(ProfileUpdateDto.class))).thenReturn(dummyResponse);

        Principal mockPrincipal = () -> "test@example.com";

        mockMvc.perform(put("/api/profile")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Tester"));
    }
}