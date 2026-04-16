package id.ac.ui.cs.advprog.bidmartauthservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwoFactorAuthServiceTest {

    private TwoFactorAuthService twoFactorAuthService;

    @BeforeEach
    void setUp() {
        twoFactorAuthService = new TwoFactorAuthService();
    }

    @Test
    void testGenerateNewSecret() {
        String secret = twoFactorAuthService.generateNewSecret();
        assertNotNull(secret);
        assertFalse(secret.isEmpty());
    }

    @Test
    void testGenerateQrCodeImageUri() {
        String secret = "JBSWY3DPEHPK3PXP";
        String email = "test@ui.ac.id";
        String uri = twoFactorAuthService.generateQrCodeImageUri(secret, email);

        assertNotNull(uri);
        assertTrue(uri.startsWith("data:image/png;base64,"));
    }

    @Test
    void testIsOtpValid_InvalidCode() {
        String secret = "JBSWY3DPEHPK3PXP";
        String code = "123456";

        boolean isValid = twoFactorAuthService.isOtpValid(secret, code);
        assertFalse(isValid);
    }
}