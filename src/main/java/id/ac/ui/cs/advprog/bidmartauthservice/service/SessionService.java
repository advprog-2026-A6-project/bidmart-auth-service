package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.SessionResponseDto;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.UserSession;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    public List<SessionResponseDto> getActiveSessions(String email) {
        User user = findUserByEmail(email);
        return userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    public void revokeSession(String email, Long sessionId) {
        User user = findUserByEmail(email);
        UserSession session = userSessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sesi tidak ditemukan"));

        if (!session.isActive()) {
            throw new IllegalStateException("Sesi sudah tidak aktif");
        }

        session.setActive(false);
        userSessionRepository.save(session);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan"));
    }

    private SessionResponseDto toDto(UserSession session) {
        return SessionResponseDto.builder()
                .id(session.getId())
                .sessionTokenId(session.getSessionTokenId())
                .deviceId(session.getDeviceId())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .active(session.isActive())
                .build();
    }
}
