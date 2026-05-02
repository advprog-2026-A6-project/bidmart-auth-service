package id.ac.ui.cs.advprog.bidmartauthservice.repository;

import id.ac.ui.cs.advprog.bidmartauthservice.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    List<UserSession> findByUserIdAndIsActiveTrueOrderByExpiresAtAsc(Long userId);

    Optional<UserSession> findTopByUserIdAndDeviceIdOrderByIdDesc(Long userId, String deviceId);

}