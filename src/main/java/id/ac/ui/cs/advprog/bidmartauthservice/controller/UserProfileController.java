package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.CodeRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.ProfileUpdateDto;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.SessionResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.service.SessionService;
import id.ac.ui.cs.advprog.bidmartauthservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SessionService sessionService;

    @PreAuthorize("hasAuthority('profile:read')")
    @GetMapping
    public ResponseEntity<ProfileResponseDto> getProfile(Principal principal) {
        ProfileResponseDto profile = userProfileService.getProfile(principal.getName());
        return ResponseEntity.ok(profile);
    }

    @PreAuthorize("hasAuthority('profile:update')")
    @PutMapping
    public ResponseEntity<ProfileResponseDto> updateProfile(Principal principal, @RequestBody ProfileUpdateDto updateDto) {
        ProfileResponseDto updatedProfile = userProfileService.updateProfile(principal.getName(), updateDto);
        return ResponseEntity.ok(updatedProfile);
    }

    @PreAuthorize("hasAuthority('profile:2fa:manage')")
    @GetMapping("/2fa/generate")
    public ResponseEntity<?> generate2faQrCode(Principal principal) {
        String qrCodeUri = userProfileService.generate2faQrCode(principal.getName());
        return ResponseEntity.ok(Map.of("qrCodeUri", qrCodeUri));
    }

    @PreAuthorize("hasAuthority('profile:2fa:manage')")
    @PostMapping({"/2fa/enable", "/2fa/enable/totp"})
    public ResponseEntity<?> enableTotp2fa(Principal principal, @RequestBody CodeRequest request) {
        boolean isEnabled = userProfileService.enableTotp2fa(principal.getName(), request.getCode());

        if (isEnabled) {
            return ResponseEntity.ok(Map.of("message", "2FA berhasil diaktifkan!"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Kode OTP salah atau kedaluwarsa. Silakan coba lagi."));
        }
    }

    @PreAuthorize("hasAuthority('profile:2fa:manage')")
    @PostMapping("/2fa/enable/email")
    public ResponseEntity<ProfileResponseDto> enableEmail2fa(Principal principal) {
        return ResponseEntity.ok(userProfileService.enableEmail2fa(principal.getName()));
    }

    @PreAuthorize("hasAuthority('profile:2fa:manage')")
    @PostMapping("/2fa/disable")
    public ResponseEntity<ProfileResponseDto> disable2fa(Principal principal) {
        return ResponseEntity.ok(userProfileService.disable2fa(principal.getName()));
    }

    @PreAuthorize("hasAuthority('session:self:read')")
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponseDto>> getActiveSessions(Principal principal) {
        return ResponseEntity.ok(sessionService.getActiveSessions(principal.getName()));
    }

    @PreAuthorize("hasAuthority('session:self:revoke')")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(Principal principal, @PathVariable Long sessionId) {
        sessionService.revokeSession(principal.getName(), sessionId);
        return ResponseEntity.ok(Map.of("message", "Sesi berhasil dicabut."));
    }
}
