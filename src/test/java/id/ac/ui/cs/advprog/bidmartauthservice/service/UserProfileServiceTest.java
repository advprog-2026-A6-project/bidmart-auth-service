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

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

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
                .isTwoFactorEnabled(false)
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
        assertFalse(response.isTwoFactorEnabled());

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
        assertFalse(response.isTwoFactorEnabled());

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

    @Test
    void testGenerate2faQrCode_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
        when(twoFactorAuthService.generateNewSecret()).thenReturn("SECRET_KEY");
        when(twoFactorAuthService.generateQrCodeImageUri("SECRET_KEY", dummyUser.getEmail())).thenReturn("data:image/png;base64,...");

        String qrCodeUri = userProfileService.generate2faQrCode("test@example.com");

        assertNotNull(qrCodeUri);
        assertEquals("data:image/png;base64,...", qrCodeUri);
        assertEquals("SECRET_KEY", dummyUser.getTwoFactorSecret());
        verify(userRepository, times(1)).save(dummyUser);
    }

    @Test
    void testGenerate2faQrCode_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userProfileService.generate2faQrCode("unknown@example.com");
        });
    }

    @Test
    void testEnable2fa_Success() {
        dummyUser.setTwoFactorSecret("SECRET_KEY");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
        when(twoFactorAuthService.isOtpValid("SECRET_KEY", "123456")).thenReturn(true);

        boolean result = userProfileService.enable2fa("test@example.com", "123456");

        assertTrue(result);
        assertTrue(dummyUser.isTwoFactorEnabled());
        verify(userRepository, times(1)).save(dummyUser);
    }

    @Test
    void testEnable2fa_InvalidCode() {
        dummyUser.setTwoFactorSecret("SECRET_KEY");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
        when(twoFactorAuthService.isOtpValid("SECRET_KEY", "000000")).thenReturn(false);

        boolean result = userProfileService.enable2fa("test@example.com", "000000");

        assertFalse(result);
        assertFalse(dummyUser.isTwoFactorEnabled());
        verify(userRepository, never()).save(dummyUser);
    }

    @Test
    void testEnable2fa_NotInitialized() {
        dummyUser.setTwoFactorSecret(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userProfileService.enable2fa("test@example.com", "123456");
        });

        assertEquals("2FA belum diinisialisasi", exception.getMessage());
    }

    @Test
    void testEnable2fa_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userProfileService.enable2fa("unknown@example.com", "123456");
        });
    }
}