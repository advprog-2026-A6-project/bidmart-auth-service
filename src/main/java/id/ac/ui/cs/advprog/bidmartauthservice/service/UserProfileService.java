package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    public ProfileResponseDto getProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
        return mapToDto(user);
    }

    public ProfileResponseDto updateProfile(String email, ProfileUpdateDto request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));

        user.setName(request.getName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setBio(request.getBio());
        user.setProfilePictureUrl(request.getProfilePictureUrl());

        userRepository.save(user);

        return mapToDto(user);
    }

    private ProfileResponseDto mapToDto(User user) {
        return ProfileResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }
}