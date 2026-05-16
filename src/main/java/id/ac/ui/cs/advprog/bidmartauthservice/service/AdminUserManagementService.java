package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.UserSession;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private static final String DEFAULT_DEACTIVATION_REASON = "Dinonaktifkan oleh administrator";

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthEventPublisherService authEventPublisherService;

    @Transactional
    public User deactivateUser(Long userId, String reason, String actorEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (!user.isActive()) {
            throw new IllegalStateException("Akun pengguna sudah dinonaktifkan");
        }

        String deactivationReason = normalizeReason(reason);
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);

        for (UserSession session : activeSessions) {
            session.setActive(false);
        }

        if (!activeSessions.isEmpty()) {
            userSessionRepository.saveAll(activeSessions);
        }

        user.setActive(false);
        user.setDeactivatedAt(LocalDateTime.now());
        user.setDeactivationReason(deactivationReason);
        User savedUser = userRepository.save(user);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", savedUser.getId());
        payload.put("email", savedUser.getEmail());
        payload.put("reason", deactivationReason);
        payload.put("deactivatedBy", actorEmail);
        payload.put("revokedSessionCount", activeSessions.size());
        payload.put("deactivatedAt", savedUser.getDeactivatedAt().toString());

        authEventPublisherService.publish(
                AuthEventType.ACCOUNT_DISABLED,
                "USER",
                savedUser.getId().toString(),
                payload
        );

        return savedUser;
    }

    private String normalizeReason(String reason) {
        return (reason == null || reason.isBlank()) ? DEFAULT_DEACTIVATION_REASON : reason.trim();
    }
}
