package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.SellerPublicProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.UserContactPreferencesResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}/public-profile")
    public ResponseEntity<SellerPublicProfileResponseDto> getPublicSellerProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userProfileService.getPublicSellerProfile(userId));
    }

    @GetMapping("/{userId}/contact-preferences")
    public ResponseEntity<UserContactPreferencesResponseDto> getContactPreferences(@PathVariable Long userId) {
        return ResponseEntity.ok(userProfileService.getContactPreferences(userId));
    }
}
