package id.ac.ui.cs.advprog.bidmartauthservice.service;

import io.jsonwebtoken.ExpiredJwtException;
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

        String token = jwtService.generateAccessToken(userDetails);
        assertNotNull(token);

        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("aaron.test@gmail.com", extractedUsername);
    }

    @Test
    void testGenerateRefreshToken() {
        when(userDetails.getUsername()).thenReturn("aaron.test@gmail.com");

        String refreshToken = jwtService.generateRefreshToken(userDetails);
        assertNotNull(refreshToken);

        String extractedUsername = jwtService.extractUsername(refreshToken);
        assertEquals("aaron.test@gmail.com", extractedUsername);
    }

    @Test
    void testIsTokenValid() {
        when(userDetails.getUsername()).thenReturn("aaron.test@gmail.com");

        String token = jwtService.generateAccessToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertTrue(isValid);
    }

    @Test
    void testIsTokenInvalidForDifferentUser() {
        when(userDetails.getUsername()).thenReturn("aaron.test@gmail.com");
        String token = jwtService.generateAccessToken(userDetails);

        when(userDetails.getUsername()).thenReturn("hacker@gmail.com");

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertFalse(isValid);
    }
}