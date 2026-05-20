package id.ac.ui.cs.advprog.bidmartauthservice.config;

import id.ac.ui.cs.advprog.bidmartauthservice.model.EmailDeliveryMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.email")
public class AuthEmailProperties {

    private EmailDeliveryMode deliveryMode = EmailDeliveryMode.SMTP;
    private String fromAddress = "no-reply@bidmart.local";
    private String applicationName = "BidMart";
}
