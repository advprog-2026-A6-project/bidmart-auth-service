package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.SellerPublicProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.UserContactPreferencesResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.model.PreferredContactMethod;
import id.ac.ui.cs.advprog.bidmartauthservice.model.TwoFactorMethod;
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
                .emailVerified(true)
                .phoneNumber("08123456789")
                .address("Jl. Testing No. 1")
                .bio("I am a tester")
                .profilePictureUrl("https://example.com/pic.jpg")
                .preferredContactMethod(PreferredContactMethod.EMAIL)
                .emailNotificationsEnabled(true)
                .pushNotificationsEnabled(false)
                .isTwoFactorEnabled(false)
                .twoFactorMethod(TwoFactorMethod.NONE)
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
        assertTrue(response.isEmailVerified());
        assertEquals("EMAIL", response.getPreferredContactMethod());
        assertTrue(response.isEmailNotificationsEnabled());
        assertFalse(response.isPushNotificationsEnabled());
        assertFalse(response.isTwoFactorEnabled());
        assertEquals("NONE", response.getTwoFactorMethod());

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
                .preferredContactMethod(PreferredContactMethod.PHONE)
                .emailNotificationsEnabled(false)
                .pushNotificationsEnabled(true)
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
        assertEquals("PHONE", response.getPreferredContactMethod());
        assertFalse(response.isEmailNotificationsEnabled());
        assertTrue(response.isPushNotificationsEnabled());
        assertFalse(response.isTwoFactorEnabled());
        assertEquals("NONE", response.getTwoFactorMethod());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testGetPublicSellerProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(dummyUser));

        SellerPublicProfileResponseDto response = userProfileService.getPublicSellerProfile(1L);

        assertNotNull(response);
        assertEquals(dummyUser.getId(), response.getId());
        assertEquals(dummyUser.getName(), response.getName());
        assertEquals(dummyUser.getBio(), response.getBio());
        assertEquals(dummyUser.getProfilePictureUrl(), response.getProfilePictureUrl());
    }

    @Test
    void testGetPublicSellerProfile_InactiveSeller() {
        dummyUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(dummyUser));

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userProfileService.getPublicSellerProfile(1L);
        });

        assertEquals("Seller tidak tersedia", exception.getMessage());
    }

    @Test
    void testGetContactPreferences_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(dummyUser));

        UserContactPreferencesResponseDto response = userProfileService.getContactPreferences(1L);

        assertNotNull(response);
        assertEquals(dummyUser.getId(), response.getUserId());
        assertEquals(dummyUser.getEmail(), response.getEmail());
        assertEquals("EMAIL", response.getPreferredContactMethod());
        assertTrue(response.isEmailNotificationsEnabled());
        assertFalse(response.isPushNotificationsEnabled());
    }

    @Test
    void testGetContactPreferences_InactiveUser() {
        dummyUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(dummyUser));

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userProfileService.getContactPreferences(1L);
        });

        assertEquals("User tidak tersedia", exception.getMessage());
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
    void testEnableTotp2fa_Success() {
        dummyUser.setTwoFactorSecret("SECRET_KEY");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
        when(twoFactorAuthService.isOtpValid("SECRET_KEY", "123456")).thenReturn(true);

        boolean result = userProfileService.enableTotp2fa("test@example.com", "123456");

        assertTrue(result);
        assertTrue(dummyUser.isTwoFactorEnabled());
        assertEquals(TwoFactorMethod.TOTP, dummyUser.getTwoFactorMethod());
        verify(userRepository, times(1)).save(dummyUser);
    }

    @Test
    void testEnableTotp2fa_InvalidCode() {
        dummyUser.setTwoFactorSecret("SECRET_KEY");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
        when(twoFactorAuthService.isOtpValid("SECRET_KEY", "000000")).thenReturn(false);

        boolean result = userProfileService.enableTotp2fa("test@example.com", "000000");

        assertFalse(result);
        assertFalse(dummyUser.isTwoFactorEnabled());
        verify(userRepository, never()).save(dummyUser);
    }

    @Test
    void testEnableTotp2fa_NotInitialized() {
        dummyUser.setTwoFactorSecret(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userProfileService.enableTotp2fa("test@example.com", "123456");
        });

        assertEquals("2FA belum diinisialisasi", exception.getMessage());
    }

    @Test
    void testEnableTotp2fa_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userProfileService.enableTotp2fa("unknown@example.com", "123456");
        });
    }

    @Test
    void testEnableEmail2fa_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
        when(userRepository.save(any(User.class))).thenReturn(dummyUser);

        ProfileResponseDto response = userProfileService.enableEmail2fa("test@example.com");

        assertNotNull(response);
        assertTrue(dummyUser.isTwoFactorEnabled());
        assertEquals(TwoFactorMethod.EMAIL, dummyUser.getTwoFactorMethod());
    }

    @Test
    void testDisable2fa_Success() {
        dummyUser.setTwoFactorEnabled(true);
        dummyUser.setTwoFactorMethod(TwoFactorMethod.TOTP);
        dummyUser.setTwoFactorSecret("SECRET_KEY");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(dummyUser));
        when(userRepository.save(any(User.class))).thenReturn(dummyUser);

        ProfileResponseDto response = userProfileService.disable2fa("test@example.com");

        assertNotNull(response);
        assertFalse(dummyUser.isTwoFactorEnabled());
        assertEquals(TwoFactorMethod.NONE, dummyUser.getTwoFactorMethod());
        assertNull(dummyUser.getTwoFactorSecret());
    }
}
