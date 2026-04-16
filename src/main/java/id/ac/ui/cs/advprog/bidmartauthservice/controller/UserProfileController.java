package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

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
    public ResponseEntity<ProfileResponseDto> updateProfile(Principal principal, @RequestBody ProfileUpdateDto updateDto) {
        ProfileResponseDto updatedProfile = userProfileService.updateProfile(principal.getName(), updateDto);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/2fa/generate")
    public ResponseEntity<?> generate2faQrCode(Principal principal) {
        String qrCodeUri = userProfileService.generate2faQrCode(principal.getName());
        return ResponseEntity.ok(Map.of("qrCodeUri", qrCodeUri));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enable2fa(Principal principal, @RequestBody Map<String, String> request) {
        String code = request.get("code");
        boolean isEnabled = userProfileService.enable2fa(principal.getName(), code);

        if (isEnabled) {
            return ResponseEntity.ok(Map.of("message", "2FA berhasil diaktifkan!"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Kode OTP salah atau kedaluwarsa. Silakan coba lagi."));
        }
    }
}