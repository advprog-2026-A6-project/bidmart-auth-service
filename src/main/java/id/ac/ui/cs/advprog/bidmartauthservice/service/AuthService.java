package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.AuthResponse;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;

public interface AuthService {
    User register(RegisterRequest request);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    AuthResponse login(LoginRequest request);
    AuthResponse verify2fa(String challengeToken, String code);
    AuthResponse refresh(String refreshToken);
    void logout(String bearerToken);
}
