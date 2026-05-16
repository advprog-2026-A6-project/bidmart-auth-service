package id.ac.ui.cs.advprog.bidmartauthservice.service;

public interface EmailDeliveryService {
    void sendVerificationEmail(String email, String token);

    void sendTwoFactorCode(String email, String code);
}
