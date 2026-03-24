package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<ProfileResponseDto> getProfile(Principal principal) {
        ProfileResponseDto profile = userProfileService.getProfile(principal.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<ProfileResponseDto> updateProfile(
            Principal principal,
            @RequestBody ProfileUpdateDto updateDto) {
        ProfileResponseDto updatedProfile = userProfileService.updateProfile(principal.getName(), updateDto);
        return ResponseEntity.ok(updatedProfile);
    }
}