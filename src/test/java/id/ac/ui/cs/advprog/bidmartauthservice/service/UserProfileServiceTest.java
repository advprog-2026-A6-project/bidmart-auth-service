package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private User dummyUser;

    @BeforeEach
    void setUp() {
        dummyUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashedpassword")
                .name("Tester")
                .phoneNumber("08123456789")
                .address("Jl. Testing No. 1")
                .bio("I am a tester")
                .profilePictureUrl("https://example.com/pic.jpg")
                .build();
    }

    @Test
    void testGetProfile_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));

        ProfileResponseDto response = userProfileService.getProfile("test@example.com");

        assertNotNull(response);
        assertEquals(dummyUser.getEmail(), response.getEmail());
        assertEquals(dummyUser.getName(), response.getName());
        assertEquals(dummyUser.getPhoneNumber(), response.getPhoneNumber());

        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void testGetProfile_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userProfileService.getProfile("unknown@example.com");
        });
    }

    @Test
    void testUpdateProfile_Success() {
        ProfileUpdateDto updateDto = ProfileUpdateDto.builder()
                .name("Tester Updated")
                .phoneNumber("08999999999")
                .address("Jl. Baru No. 2")
                .bio("Updated bio")
                .profilePictureUrl("https://example.com/newpic.jpg")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
        when(userRepository.save(any(User.class))).thenReturn(dummyUser);

        ProfileResponseDto response = userProfileService.updateProfile("test@example.com", updateDto);

        assertNotNull(response);
        assertEquals("Tester Updated", response.getName());
        assertEquals("08999999999", response.getPhoneNumber());
        assertEquals("Jl. Baru No. 2", response.getAddress());
        assertEquals("Updated bio", response.getBio());
        assertEquals("https://example.com/newpic.jpg", response.getProfilePictureUrl());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateProfile_UserNotFound() {
        ProfileUpdateDto updateDto = new ProfileUpdateDto();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userProfileService.updateProfile("unknown@example.com", updateDto);
        });

        verify(userRepository, never()).save(any(User.class));
    }
}