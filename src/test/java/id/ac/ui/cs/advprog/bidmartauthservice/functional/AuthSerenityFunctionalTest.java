package id.ac.ui.cs.advprog.bidmartauthservice.functional;

import id.ac.ui.cs.advprog.bidmartauthservice.functional.support.AdminApiSteps;
import id.ac.ui.cs.advprog.bidmartauthservice.functional.support.AuthApiSteps;
import id.ac.ui.cs.advprog.bidmartauthservice.functional.support.ProfileApiSteps;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEvent;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationPurpose;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationToken;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.AuthEventRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.VerificationTokenRepository;
import net.serenitybdd.annotations.Feature;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.annotations.Story;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SerenityJUnit5Extension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Feature("Authentication and Account Security")
class AuthSerenityFunctionalTest {

    private static final String USER_PASSWORD = "Bidmart123!";
    private static final String ADMIN_EMAIL = "admin@bidmart.com";
    private static final String ADMIN_PASSWORD = "AdminBidmart123!";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private AuthEventRepository authEventRepository;

    @Steps
    private AuthApiSteps authApi;

    @Steps
    private ProfileApiSteps profileApi;

    @Steps
    private AdminApiSteps adminApi;

    @Test
    @Story("Register, verify, and authenticate a buyer account")
    @DisplayName("buyer can register, verify email, and obtain a full authenticated session")
    void buyerCanRegisterVerifyEmailAndLogin() {
        String email = uniqueEmail("buyer");

        authApi.registerUser(port, email, "BUYER", USER_PASSWORD);
        authApi.verifyEmail(port, latestTokenFor(email, VerificationPurpose.EMAIL_VERIFICATION).getToken());

        String accessToken = authApi.login(port, email, USER_PASSWORD, "Serenity-Buyer-Device")
                .jsonPath()
                .getString("accessToken");

        Serenity.recordReportData()
                .withTitle("Verified buyer login")
                .andContents("Buyer " + email + " completed registration, email verification, and login.");

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(accessToken).isNotBlank();
        assertThat(profileApi.getProfile(port, accessToken).jsonPath().getString("email")).isEqualTo(email);
    }

    @Test
    @Story("Complete second-factor verification before getting a full session")
    @DisplayName("email 2FA challenge must be completed before the session is fully authenticated")
    void emailTwoFactorChallengeMustBeCompletedBeforeFullAuthentication() {
        String email = uniqueEmail("mfa");

        authApi.registerUser(port, email, "SELLER", USER_PASSWORD);
        authApi.verifyEmail(port, latestTokenFor(email, VerificationPurpose.EMAIL_VERIFICATION).getToken());

        String firstAccessToken = authApi.login(port, email, USER_PASSWORD, "Serenity-Mfa-Setup")
                .jsonPath()
                .getString("accessToken");

        profileApi.enableEmail2fa(port, firstAccessToken);

        String mfaUserAgent = "Serenity-Mfa-Challenge";
        var loginResponse = authApi.login(port, email, USER_PASSWORD, mfaUserAgent);
        String challengeToken = loginResponse.jsonPath().getString("mfaChallengeToken");

        VerificationToken loginChallenge = verificationTokenRepository.findByToken(challengeToken).orElseThrow();
        var verifyResponse = authApi.verifySecondFactor(port, challengeToken, loginChallenge.getCode(), mfaUserAgent);
        assertThat(verifyResponse).isNotNull();
        String finalAccessToken = verifyResponse.jsonPath()
                .getString("accessToken");

        Serenity.recordReportData()
                .withTitle("Email 2FA challenge")
                .andContents("Challenge token " + challengeToken + " produced a full session only after OTP verification.");

        assertThat(loginResponse.jsonPath().getBoolean("mfaRequired")).isTrue();
        assertThat(loginResponse.jsonPath().getString("twoFactorMethod")).isEqualTo("EMAIL");
        assertThat(finalAccessToken).isNotBlank();
        assertThat(profileApi.getProfile(port, finalAccessToken).jsonPath().getString("email")).isEqualTo(email);
    }

    @Test
    @Story("Deactivate abusive accounts and revoke every active session")
    @DisplayName("admin deactivation invalidates all active sessions for the target user")
    void adminDeactivationInvalidatesEveryActiveSession() {
        String email = uniqueEmail("deactivate");

        authApi.registerUser(port, email, "BUYER", USER_PASSWORD);
        authApi.verifyEmail(port, latestTokenFor(email, VerificationPurpose.EMAIL_VERIFICATION).getToken());

        String userAccessToken = authApi.login(port, email, USER_PASSWORD, "Serenity-Deactivate-User")
                .jsonPath()
                .getString("accessToken");
        String adminAccessToken = authApi.login(port, ADMIN_EMAIL, ADMIN_PASSWORD, "Serenity-Deactivate-Admin")
                .jsonPath()
                .getString("accessToken");

        User targetUser = userRepository.findByEmail(email).orElseThrow();

        adminApi.deactivateUser(port, targetUser.getId(), adminAccessToken, "Functional security review");
        adminApi.expectProfileUnauthorized(port, userAccessToken);

        Serenity.recordReportData()
                .withTitle("Account deactivation result")
                .andContents("User " + email + " was deactivated and their old session was rejected.");

        AuthEvent latestAccountDisabledEvent = authEventRepository.findByEventTypeOrderByCreatedAtDesc(AuthEventType.ACCOUNT_DISABLED)
                .stream()
                .filter(event -> event.getAggregateId().equals(targetUser.getId().toString()))
                .findFirst()
                .orElseThrow();

        assertThat(userRepository.findById(targetUser.getId()).orElseThrow().isActive()).isFalse();
        assertThat(userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(targetUser.getId())).isEmpty();
        assertThat(latestAccountDisabledEvent.getPayload()).contains(email);
    }

    @Step("Find latest {1} token for {0}")
    VerificationToken latestTokenFor(String email, VerificationPurpose purpose) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return verificationTokenRepository.findByUserIdAndPurposeAndConsumedFalse(user.getId(), purpose)
                .stream()
                .max(Comparator.comparing(VerificationToken::getCreatedAt))
                .orElseThrow();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@mail.com";
    }
}
