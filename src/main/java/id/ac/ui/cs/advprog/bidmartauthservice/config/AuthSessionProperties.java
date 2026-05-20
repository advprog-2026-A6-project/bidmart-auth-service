package id.ac.ui.cs.advprog.bidmartauthservice.config;

import id.ac.ui.cs.advprog.bidmartauthservice.model.SessionOverflowPolicy;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "auth.session")
public class AuthSessionProperties {

    @Min(1)
    private int maxConcurrentSessions = 3;

    @Min(1)
    private int expiryDays = 7;

    private SessionOverflowPolicy overflowPolicy = SessionOverflowPolicy.REVOKE_OLDEST;
}
