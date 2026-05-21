package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.config.AuthSessionProperties;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.SessionOverflowPolicy;
import id.ac.ui.cs.advprog.bidmartauthservice.model.TwoFactorMethod;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.UserSession;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationMethod;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationToken;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private final VerificationTokenService verificationTokenService;
    private final HttpServletRequest httpServletRequest;
    private final AuthSessionProperties authSessionProperties;

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email sudah terdaftar!");
        }

        String requestedRoleName = resolveRequestedRegistrationRole(request.getRole());
        Role requestedRole = roleRepository.findByName(requestedRoleName)
                .orElseThrow(() -> new RuntimeException(
                        "Role registrasi " + requestedRoleName + " tidak ditemukan. Pastikan DataSeeder berjalan."
                ));

        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .twoFactorMethod(TwoFactorMethod.NONE)
                .roles(new HashSet<>(Set.of(requestedRole)))
                .build();

        User savedUser = userRepository.save(newUser);
        verificationTokenService.createEmailVerification(savedUser);
        return savedUser;
    }

    private String resolveRequestedRegistrationRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return "BUYER";
        }

        String normalizedRole = requestedRole.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("BUYER", "SELLER").contains(normalizedRole)) {
            throw new IllegalArgumentException("Role registrasi hanya boleh BUYER atau SELLER");
        }

        return normalizedRole;
    }

    @Override
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenService.getValidEmailVerificationToken(token);
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokenService.markConsumed(verificationToken);
    }

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email sudah diverifikasi");
        }

        verificationTokenService.createEmailVerification(user);
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

        if (!user.isActive()) {
            throw new DisabledException("Akun pengguna telah dinonaktifkan");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalStateException("Email belum diverifikasi");
        }

        String deviceId = resolveDeviceId();

        if (user.isTwoFactorEnabled()) {
            VerificationToken challenge = verificationTokenService.createLoginChallenge(user, deviceId);
            return AuthResponse.builder()
                    .accessToken("")
                    .refreshToken("")
                    .mfaRequired(true)
                    .mfaChallengeToken(challenge.getToken())
                    .twoFactorMethod(user.getTwoFactorMethod().name())
                    .build();
        }

        return generateAuthResponseWithSession(user, deviceId);
    }

    @Override
    public AuthResponse verify2fa(String challengeToken, String code) {
        VerificationToken challenge = verificationTokenService.getValidLoginChallenge(challengeToken);
        User user = challenge.getUser();

        if (challenge.getDeviceId() != null && !challenge.getDeviceId().equals(resolveDeviceId())) {
            throw new IllegalStateException("Challenge 2FA tidak berlaku untuk perangkat ini");
        }

        boolean valid = switch (challenge.getMethod()) {
            case TOTP -> user.getTwoFactorSecret() != null
                    && twoFactorAuthService.isOtpValid(user.getTwoFactorSecret(), code);
            case EMAIL -> challenge.getCode() != null && challenge.getCode().equals(code);
            default -> false;
        };

        if (!valid) {
            throw new BadCredentialsException("Kode OTP salah atau sudah kedaluwarsa");
        }

        verificationTokenService.markConsumed(challenge);
        return generateAuthResponseWithSession(user, challenge.getDeviceId());
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token wajib diisi");
        }

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Token yang diberikan bukan refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        String sessionTokenId = jwtService.extractSessionTokenId(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token tidak valid");
        }

        UserSession session = userSessionRepository.findBySessionTokenId(sessionTokenId)
                .orElseThrow(() -> new IllegalArgumentException("Sesi tidak ditemukan"));

        if (!session.isActive() || !refreshToken.equals(session.getRefreshToken())) {
            throw new IllegalStateException("Sesi sudah dicabut");
        }

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setActive(false);
            userSessionRepository.save(session);
            throw new IllegalStateException("Refresh token sudah kedaluwarsa");
        }

        return rotateTokens(user, session);
    }

    @Override
    public void logout(String bearerToken) {
        String token = extractBearerToken(bearerToken);
        String sessionTokenId = jwtService.extractSessionTokenId(token);

        UserSession session = userSessionRepository.findBySessionTokenId(sessionTokenId)
                .orElseThrow(() -> new IllegalArgumentException("Sesi tidak ditemukan"));
        session.setActive(false);
        userSessionRepository.save(session);
    }

    private AuthResponse generateAuthResponseWithSession(User user, String deviceId) {
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtAsc(user.getId());

        if (activeSessions.size() >= authSessionProperties.getMaxConcurrentSessions()) {
            if (authSessionProperties.getOverflowPolicy() == SessionOverflowPolicy.REJECT_NEW) {
                throw new IllegalStateException("Batas sesi aktif tercapai. Silakan logout dari perangkat lain terlebih dahulu.");
            }

            UserSession oldestSession = activeSessions.get(0);
            oldestSession.setActive(false);
            userSessionRepository.save(oldestSession);
        }

        String sessionTokenId = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(user, sessionTokenId);
        String refreshToken = jwtService.generateRefreshToken(user, sessionTokenId);

        UserSession newSession = UserSession.builder()
                .user(user)
                .sessionTokenId(sessionTokenId)
                .deviceId(deviceId)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(authSessionProperties.getExpiryDays()))
                .isActive(true)
                .build();

        userSessionRepository.save(newSession);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mfaRequired(false)
                .mfaChallengeToken(null)
                .twoFactorMethod(user.getTwoFactorMethod().name())
                .build();
    }

    private AuthResponse rotateTokens(User user, UserSession session) {
        String accessToken = jwtService.generateAccessToken(user, session.getSessionTokenId());
        String refreshToken = jwtService.generateRefreshToken(user, session.getSessionTokenId());

        session.setRefreshToken(refreshToken);
        session.setExpiresAt(LocalDateTime.now().plusDays(authSessionProperties.getExpiryDays()));
        userSessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mfaRequired(false)
                .twoFactorMethod(user.getTwoFactorMethod().name())
                .build();
    }

    private String resolveDeviceId() {
        String userAgent = httpServletRequest.getHeader("User-Agent");
        return (userAgent == null || userAgent.isBlank()) ? "Unknown Device" : userAgent;
    }

    private String extractBearerToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header tidak valid");
        }
        return bearerToken.substring(7);
    }
}
