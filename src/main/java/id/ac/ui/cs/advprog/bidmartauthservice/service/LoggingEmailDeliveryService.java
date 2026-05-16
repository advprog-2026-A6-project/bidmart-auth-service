package id.ac.ui.cs.advprog.bidmartauthservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingEmailDeliveryService implements EmailDeliveryService {
    @Override
    public void sendVerificationEmail(String email, String token) {
        log.info("Verification email for {} with token {}", email, token);
    }

    @Override
    public void sendTwoFactorCode(String email, String code) {
        log.info("Two-factor code for {} is {}", email, code);
    }
}
