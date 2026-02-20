package id.ac.ui.cs.advprog.bidmartauthservice.feature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DummyUserRepository extends JpaRepository<DummyUser, Long> {
}