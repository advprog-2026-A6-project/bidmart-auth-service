package id.ac.ui.cs.advprog.bidmartauthservice.repository;

import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationPurpose;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    List<VerificationToken> findByUserIdAndPurposeAndConsumedFalse(Long userId, VerificationPurpose purpose);
}
