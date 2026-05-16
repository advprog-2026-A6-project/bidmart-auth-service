package id.ac.ui.cs.advprog.bidmartauthservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SESSION_TOKEN_ID = "session-123";

    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", "BidMartSuperSecretKeyForAuthenticationAdvProg2026PalingAmanDiDunia");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 1000 * 60 * 15L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 1000 * 60 * 60 * 24 * 7L);
    }

    @Test
    void testGenerateAndExtractAccessToken() {

        when(userDetails.getUsername()).thenReturn("aaron.test@gmail.com");

        String token = jwtService.generateAccessToken(userDetails, SESSION_TOKEN_ID);
        assertNotNull(token);

        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("aaron.test@gmail.com", extractedUsername);
        assertEquals(SESSION_TOKEN_ID, jwtService.extractSessionTokenId(token));
        assertEquals("access", jwtService.extractTokenType(token));
    }

    @Test
    void testGenerateRefreshToken() {
        when(userDetails.getUsername()).thenReturn("aaron.test@gmail.com");

        String refreshToken = jwtService.generateRefreshToken(userDetails, SESSION_TOKEN_ID);
        assertNotNull(refreshToken);

        String extractedUsername = jwtService.extractUsername(refreshToken);
        assertEquals("aaron.test@gmail.com", extractedUsername);
        assertTrue(jwtService.isRefreshToken(refreshToken));
    }

    @Test
    void testIsTokenValid() {
        when(userDetails.getUsername()).thenReturn("aaron.test@gmail.com");

        String token = jwtService.generateAccessToken(userDetails, SESSION_TOKEN_ID);

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertTrue(isValid);
    }

    @Test
    void testIsTokenInvalidForDifferentUser() {
        when(userDetails.getUsername()).thenReturn("aaron.test@gmail.com");
        String token = jwtService.generateAccessToken(userDetails, SESSION_TOKEN_ID);

        when(userDetails.getUsername()).thenReturn("hacker@gmail.com");

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertFalse(isValid);
    }
}
