package id.ac.ui.cs.advprog.bidmartauthservice.config;

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
@ConfigurationProperties(prefix = "auth.verification")
public class AuthVerificationProperties {

    @Min(1)
    private long emailExpiryHours = 24;

    @Min(1)
    private long loginChallengeExpiryMinutes = 5;
}
