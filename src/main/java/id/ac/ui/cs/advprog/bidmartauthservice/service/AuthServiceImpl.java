package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.UserSession;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TwoFactorAuthService twoFactorAuthService;
    private final HttpServletRequest httpServletRequest;

    private static final int MAX_DEVICES = 3;

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email sudah terdaftar!");
        }

        Role defaultRole = roleRepository.findByName("BUYER")
                .orElseThrow(() -> new RuntimeException("Role default BUYER tidak ditemukan. Pastikan DataSeeder berjalan."));

        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new HashSet<>(Set.of(defaultRole)))
                .build();

        return userRepository.save(newUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email atau kata sandi salah!"));

        if (user.isTwoFactorEnabled()) {
            return AuthResponse.builder()
                    .accessToken("")
                    .refreshToken("")
                    .mfaRequired(true)
                    .build();
        }

        return generateAuthResponseWithSession(user);
    }

    @Override
    public AuthResponse verify2fa(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (!twoFactorAuthService.isOtpValid(user.getTwoFactorSecret(), code)) {
            throw new BadCredentialsException("Kode OTP salah atau sudah kedaluwarsa");
        }

        return generateAuthResponseWithSession(user);
    }

    private AuthResponse generateAuthResponseWithSession(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        String userAgent = httpServletRequest.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "Unknown Device";
        }

        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndIsActiveTrueOrderByExpiresAtAsc(user.getId());

        if (activeSessions.size() >= MAX_DEVICES) {
            UserSession oldestSession = activeSessions.get(0);
            oldestSession.setActive(false);
            userSessionRepository.save(oldestSession);
        }

        UserSession newSession = UserSession.builder()
                .user(user)
                .deviceId(userAgent)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build();

        userSessionRepository.save(newSession);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mfaRequired(false)
                .build();
    }
}