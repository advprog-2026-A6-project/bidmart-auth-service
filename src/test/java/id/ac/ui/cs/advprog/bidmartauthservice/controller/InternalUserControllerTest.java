package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.SellerPublicProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.UserContactPreferencesResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.service.JwtService;
import id.ac.ui.cs.advprog.bidmartauthservice.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalUserControllerTest {

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

    @Test
    void getPublicSellerProfileReturnsSellerProfile() throws Exception {
        when(userProfileService.getPublicSellerProfile(1L)).thenReturn(
                SellerPublicProfileResponseDto.builder()
                        .id(1L)
                        .name("Seller One")
                        .bio("Trusted seller")
                        .profilePictureUrl("https://example.com/seller.jpg")
                        .build()
        );

        mockMvc.perform(get("/api/internal/users/1/public-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Seller One"))
                .andExpect(jsonPath("$.bio").value("Trusted seller"))
                .andExpect(jsonPath("$.profilePictureUrl").value("https://example.com/seller.jpg"));
    }

    @Test
    void getPublicSellerProfileReturnsNotFoundWhenSellerMissing() throws Exception {
        when(userProfileService.getPublicSellerProfile(99L))
                .thenThrow(new UsernameNotFoundException("Seller tidak tersedia"));

        mockMvc.perform(get("/api/internal/users/99/public-profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Seller tidak tersedia"));
    }

    @Test
    void getContactPreferencesReturnsUserPreferences() throws Exception {
        when(userProfileService.getContactPreferences(1L)).thenReturn(
                UserContactPreferencesResponseDto.builder()
                        .userId(1L)
                        .email("seller@example.com")
                        .preferredContactMethod("EMAIL")
                        .emailNotificationsEnabled(true)
                        .pushNotificationsEnabled(false)
                        .build()
        );

        mockMvc.perform(get("/api/internal/users/1/contact-preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("seller@example.com"))
                .andExpect(jsonPath("$.preferredContactMethod").value("EMAIL"))
                .andExpect(jsonPath("$.emailNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.pushNotificationsEnabled").value(false));
    }

}
