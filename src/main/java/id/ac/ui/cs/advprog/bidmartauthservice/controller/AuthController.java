package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.EmailRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.RefreshTokenRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.VerifyEmailRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.Verify2faRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.ok(Map.of("message", "Registrasi berhasil! Silakan cek email untuk verifikasi akun."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(Map.of("message", "Email berhasil diverifikasi."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody EmailRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Email verifikasi berhasil dikirim ulang."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<AuthResponse> verify2fa(@RequestBody Verify2faRequest request) {
        return ResponseEntity.ok(authService.verify2fa(request.getChallengeToken(), request.getCode()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(Map.of("message", "Logout berhasil."));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(org.springframework.security.core.Authentication authentication) {
        id.ac.ui.cs.advprog.bidmartauthservice.model.User user = (id.ac.ui.cs.advprog.bidmartauthservice.model.User) authentication.getPrincipal();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .sorted()
                .toList();
        List<String> roles = user.getRoles().stream()
                .map(id.ac.ui.cs.advprog.bidmartauthservice.model.Role::getName)
                .sorted()
                .toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(id.ac.ui.cs.advprog.bidmartauthservice.model.Permission::getName)
                .distinct()
                .sorted()
                .toList();
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "userId", user.getId(),
                "email", user.getEmail(),
                "active", user.isActive(),
                "disabled", !user.isActive(),
                "roles", roles,
                "permissions", permissions,
                "authorities", authorities
        ));
    }
}
