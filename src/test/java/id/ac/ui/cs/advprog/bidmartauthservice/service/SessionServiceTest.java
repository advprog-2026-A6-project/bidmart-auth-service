package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.SessionResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.UserSession;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private SessionService sessionService;

    private User user;
    private UserSession session1;
    private UserSession session2;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .build();

        session1 = UserSession.builder()
                .id(100L)
                .user(user)
                .sessionTokenId("token-1")
                .deviceId("device-1")
                .createdAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .build();

        session2 = UserSession.builder()
                .id(200L)
                .user(user)
                .sessionTokenId("token-2")
                .deviceId("device-2")
                .createdAt(LocalDateTime.now().minusHours(2))
                .expiresAt(LocalDateTime.now().plusDays(2))
                .isActive(true)
                .build();
    }

    @Test
    void getActiveSessions_UserFound_ReturnsDtoList() {
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(session2, session1));

        List<SessionResponseDto> dtos = sessionService.getActiveSessions("test@mail.com");

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo(200L);
        assertThat(dtos.get(1).getId()).isEqualTo(100L);
    }

    @Test
    void getActiveSessions_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> sessionService.getActiveSessions("notfound@mail.com"));
        verify(userSessionRepository, never()).findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(any());
    }

    @Test
    void revokeSession_ValidSession_SetsInactiveAndSaves() {
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(userSessionRepository.findByIdAndUserId(100L, user.getId())).thenReturn(Optional.of(session1));

        sessionService.revokeSession("test@mail.com", 100L);

        assertThat(session1.isActive()).isFalse();
        verify(userSessionRepository).save(session1);
    }

    @Test
    void revokeSession_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> sessionService.revokeSession("notfound@mail.com", 100L));
    }

    @Test
    void revokeSession_SessionNotFound_ThrowsException() {
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(userSessionRepository.findByIdAndUserId(999L, user.getId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> sessionService.revokeSession("test@mail.com", 999L));
        assertThat(ex.getMessage()).isEqualTo("Sesi tidak ditemukan");
    }

    @Test
    void revokeSession_SessionAlreadyInactive_ThrowsException() {
        session1.setActive(false);
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(userSessionRepository.findByIdAndUserId(100L, user.getId())).thenReturn(Optional.of(session1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> sessionService.revokeSession("test@mail.com", 100L));
        assertThat(ex.getMessage()).isEqualTo("Sesi sudah tidak aktif");
    }
}
