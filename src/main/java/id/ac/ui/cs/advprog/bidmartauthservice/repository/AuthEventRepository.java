package id.ac.ui.cs.advprog.bidmartauthservice.repository;

import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEvent;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthEventRepository extends JpaRepository<AuthEvent, Long> {

    List<AuthEvent> findByEventTypeOrderByCreatedAtDesc(AuthEventType eventType);

    List<AuthEvent> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

    Optional<AuthEvent> findByIdAndPublishedAtIsNull(Long id);

}
