package id.ac.ui.cs.advprog.bidmartauthservice.repository;

import id.ac.ui.cs.advprog.bidmartauthservice.model.UserSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    List<UserSession> findByUserIdAndIsActiveTrueOrderByCreatedAtAsc(Long userId);

    List<UserSession> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<UserSession> findBySessionTokenId(String sessionTokenId);

    Optional<UserSession> findByIdAndUserId(Long id, Long userId);

}
