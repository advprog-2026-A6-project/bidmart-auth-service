package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.config.AuthVerificationProperties;
import id.ac.ui.cs.advprog.bidmartauthservice.model.*;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailDeliveryService emailDeliveryService;
    private final AuthVerificationProperties authVerificationProperties;

    public void createEmailVerification(User user) {
        invalidateOpenTokens(user.getId(), VerificationPurpose.EMAIL_VERIFICATION);

        VerificationToken token = verificationTokenRepository.save(VerificationToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .purpose(VerificationPurpose.EMAIL_VERIFICATION)
                .method(VerificationMethod.LINK)
                .expiresAt(LocalDateTime.now().plusHours(authVerificationProperties.getEmailExpiryHours()))
                .build());

        emailDeliveryService.sendVerificationEmail(user.getEmail(), token.getToken());
    }

    public VerificationToken createLoginChallenge(User user, String deviceId) {
        invalidateOpenTokens(user.getId(), VerificationPurpose.LOGIN_2FA);

        VerificationMethod method = user.getTwoFactorMethod() == TwoFactorMethod.EMAIL
                ? VerificationMethod.EMAIL
                : VerificationMethod.TOTP;
        String code = method == VerificationMethod.EMAIL ? generateNumericCode() : null;

        VerificationToken challenge = verificationTokenRepository.save(VerificationToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .code(code)
                .purpose(VerificationPurpose.LOGIN_2FA)
                .method(method)
                .deviceId(deviceId)
                .expiresAt(LocalDateTime.now().plusMinutes(authVerificationProperties.getLoginChallengeExpiryMinutes()))
                .build());

        if (code != null) {
            emailDeliveryService.sendTwoFactorCode(user.getEmail(), code);
        }

        return challenge;
    }

    public VerificationToken getValidEmailVerificationToken(String token) {
        return getValidToken(token, VerificationPurpose.EMAIL_VERIFICATION);
    }

    public VerificationToken getValidLoginChallenge(String token) {
        return getValidToken(token, VerificationPurpose.LOGIN_2FA);
    }

    public void markConsumed(VerificationToken token) {
        token.setConsumed(true);
        verificationTokenRepository.save(token);
    }

    private VerificationToken getValidToken(String token, VerificationPurpose purpose) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token verifikasi tidak ditemukan"));

        if (verificationToken.getPurpose() != purpose) {
            throw new IllegalArgumentException("Token verifikasi tidak sesuai");
        }
        if (verificationToken.isConsumed()) {
            throw new IllegalStateException("Token verifikasi sudah digunakan");
        }
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token verifikasi sudah kedaluwarsa");
        }

        return verificationToken;
    }

    private void invalidateOpenTokens(Long userId, VerificationPurpose purpose) {
        List<VerificationToken> tokens = verificationTokenRepository
                .findByUserIdAndPurposeAndConsumedFalse(userId, purpose);

        for (VerificationToken token : tokens) {
            token.setConsumed(true);
        }

        if (!tokens.isEmpty()) {
            verificationTokenRepository.saveAll(tokens);
        }
    }

    private String generateNumericCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return Integer.toString(code);
    }
}
