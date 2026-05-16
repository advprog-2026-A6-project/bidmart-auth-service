package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.model.TwoFactorMethod;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final TwoFactorAuthService twoFactorAuthService;

    public ProfileResponseDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

        return ProfileResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isTwoFactorEnabled(user.isTwoFactorEnabled())
                .twoFactorMethod(user.getTwoFactorMethod().name())
                .build();
    }

    public ProfileResponseDto updateProfile(String email, ProfileUpdateDto updateDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

        user.setName(updateDto.getName());
        user.setPhoneNumber(updateDto.getPhoneNumber());
        user.setAddress(updateDto.getAddress());
        user.setBio(updateDto.getBio());
        user.setProfilePictureUrl(updateDto.getProfilePictureUrl());

        User updatedUser = userRepository.save(user);

        return ProfileResponseDto.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .emailVerified(updatedUser.isEmailVerified())
                .name(updatedUser.getName())
                .phoneNumber(updatedUser.getPhoneNumber())
                .address(updatedUser.getAddress())
                .bio(updatedUser.getBio())
                .profilePictureUrl(updatedUser.getProfilePictureUrl())
                .isTwoFactorEnabled(updatedUser.isTwoFactorEnabled())
                .twoFactorMethod(updatedUser.getTwoFactorMethod().name())
                .build();
    }

    public String generate2faQrCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

        String secret = twoFactorAuthService.generateNewSecret();
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        return twoFactorAuthService.generateQrCodeImageUri(secret, user.getEmail());
    }

    public boolean enableTotp2fa(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

        if (user.getTwoFactorSecret() == null) {
            throw new IllegalStateException("2FA belum diinisialisasi");
        }

        boolean isValid = twoFactorAuthService.isOtpValid(user.getTwoFactorSecret(), code);

        if (isValid) {
            user.setTwoFactorEnabled(true);
            user.setTwoFactorMethod(TwoFactorMethod.TOTP);
            userRepository.save(user);
            return true;
        }

        return false;
    }

    public ProfileResponseDto enableEmail2fa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

        if (!user.isEmailVerified()) {
            throw new IllegalStateException("Email harus diverifikasi sebelum mengaktifkan 2FA email");
        }

        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod(TwoFactorMethod.EMAIL);
        user.setTwoFactorSecret(null);

        return mapToResponse(userRepository.save(user));
    }

    public ProfileResponseDto disable2fa(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

        user.setTwoFactorEnabled(false);
        user.setTwoFactorMethod(TwoFactorMethod.NONE);
        user.setTwoFactorSecret(null);

        return mapToResponse(userRepository.save(user));
    }

    private ProfileResponseDto mapToResponse(User user) {
        return ProfileResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isTwoFactorEnabled(user.isTwoFactorEnabled())
                .twoFactorMethod(user.getTwoFactorMethod().name())
                .build();
    }
}
