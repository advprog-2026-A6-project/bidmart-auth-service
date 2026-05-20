package id.ac.ui.cs.advprog.bidmartauthservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSessionTest {

    private UserSession userSession;
    private User user;
    private LocalDateTime time;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@mail.com").build();
        time = LocalDateTime.now();

        userSession = UserSession.builder()
                .id(1L)
                .user(user)
                .sessionTokenId("session-123")
                .deviceId("Device-1")
                .refreshToken("dummy_refresh_token")
                .createdAt(time.minusHours(1))
                .expiresAt(time)
                .isActive(true)
                .build();
    }

    @Test
    void testUserSessionGetters() {
        assertEquals(1L, userSession.getId());
        assertEquals(user, userSession.getUser());
        assertEquals("session-123", userSession.getSessionTokenId());
        assertEquals("Device-1", userSession.getDeviceId());
        assertEquals("dummy_refresh_token", userSession.getRefreshToken());
        assertEquals(time.minusHours(1), userSession.getCreatedAt());
        assertEquals(time, userSession.getExpiresAt());
        assertTrue(userSession.isActive());
    }

    @Test
    void testUserSessionSetters() {
        UserSession emptySession = new UserSession();
        User newUser = User.builder().id(2L).build();
        LocalDateTime newTime = LocalDateTime.now().plusDays(1);

        emptySession.setId(2L);
        emptySession.setUser(newUser);
        emptySession.setSessionTokenId("session-456");
        emptySession.setDeviceId("Device-2");
        emptySession.setRefreshToken("new_token");
        emptySession.setCreatedAt(newTime.minusHours(1));
        emptySession.setExpiresAt(newTime);
        emptySession.setActive(false);

        assertEquals(2L, emptySession.getId());
        assertEquals(newUser, emptySession.getUser());
        assertEquals("session-456", emptySession.getSessionTokenId());
        assertEquals("Device-2", emptySession.getDeviceId());
        assertEquals("new_token", emptySession.getRefreshToken());
        assertEquals(newTime.minusHours(1), emptySession.getCreatedAt());
        assertEquals(newTime, emptySession.getExpiresAt());
        assertFalse(emptySession.isActive());
    }
}
