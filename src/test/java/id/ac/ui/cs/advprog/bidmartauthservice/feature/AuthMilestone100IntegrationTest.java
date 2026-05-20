package id.ac.ui.cs.advprog.bidmartauthservice.feature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.LoginRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.RegisterRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEvent;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationPurpose;
import id.ac.ui.cs.advprog.bidmartauthservice.model.VerificationToken;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.AuthEventRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserSessionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthMilestone100IntegrationTest {

    private static final String DEFAULT_PASSWORD = "Bidmart123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private AuthEventRepository authEventRepository;

    @Test
    void adminCanDeactivateUserInvalidateSessionsAndPersistEvent() throws Exception {
        User user = registerAndVerifyUser("deactivate-" + UUID.randomUUID() + "@mail.com");
        String userAccessToken = loginAndGetAccessToken(user.getEmail(), DEFAULT_PASSWORD, "Buyer-Test-Device");

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));

        String adminAccessToken = loginAndGetAccessToken("admin@bidmart.com", "AdminBidmart123!", "Admin-Test-Device");

        mockMvc.perform(post("/api/admin/users/{userId}/deactivate", user.getId())
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Pelanggaran aturan lelang"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.active").value(false));

        User deactivatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(deactivatedUser.isActive());
        assertNotNull(deactivatedUser.getDeactivatedAt());
        assertThat(userSessionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(user.getId())).isEmpty();

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + userAccessToken))
                .andExpect(status().isUnauthorized());

        AuthEvent accountDisabledEvent = authEventRepository.findByEventTypeOrderByCreatedAtDesc(AuthEventType.ACCOUNT_DISABLED)
                .stream()
                .filter(event -> event.getAggregateId().equals(user.getId().toString()))
                .findFirst()
                .orElseThrow();

        assertNotNull(accountDisabledEvent.getPublishedAt());
        assertThat(accountDisabledEvent.getPayload()).contains(user.getEmail());
        assertThat(accountDisabledEvent.getPayload()).contains("Pelanggaran aturan lelang");
    }

    @Test
    void nonAdminCannotDeactivateUser() throws Exception {
        User user = registerAndVerifyUser("nonadmin-" + UUID.randomUUID() + "@mail.com");
        User targetUser = registerAndVerifyUser("target-" + UUID.randomUUID() + "@mail.com");
        String userAccessToken = loginAndGetAccessToken(user.getEmail(), DEFAULT_PASSWORD, "Buyer-No-Admin");

        mockMvc.perform(post("/api/admin/users/{userId}/deactivate", targetUser.getId())
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Tidak boleh"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Akses ditolak"));
    }

    @Test
    void adminRbacChangesPersistPublishedEvents() throws Exception {
        User user = registerAndVerifyUser("rbac-" + UUID.randomUUID() + "@mail.com");
        String adminAccessToken = loginAndGetAccessToken("admin@bidmart.com", "AdminBidmart123!", "Admin-Rbac-Device");

        String permissionName = "wallet:read:" + UUID.randomUUID().toString().substring(0, 8);
        String roleName = "AUDITOR_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        mockMvc.perform(post("/api/admin/rbac/permissions")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", permissionName))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(permissionName));

        mockMvc.perform(post("/api/admin/rbac/roles")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", roleName, "permissions", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(roleName));

        mockMvc.perform(post("/api/admin/rbac/roles/{roleName}/permissions/{permissionName}", roleName, permissionName)
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(roleName));

        mockMvc.perform(post("/api/admin/rbac/users/{userId}/roles/{roleName}", user.getId(), roleName)
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()));

        assertPublishedEvent(AuthEventType.PERMISSION_CREATED, permissionName);
        assertPublishedEvent(AuthEventType.ROLE_CREATED, roleName);
        assertPublishedEvent(AuthEventType.ROLE_PERMISSION_CHANGED, roleName);
        assertPublishedEvent(AuthEventType.USER_ROLE_CHANGED, user.getId().toString());
    }

    @Test
    void actuatorHealthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private User registerAndVerifyUser(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Integration User");
        registerRequest.setEmail(email);
        registerRequest.setPassword(DEFAULT_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(email).orElseThrow();
        VerificationToken verificationToken = verificationTokenRepository
                .findByUserIdAndPurposeAndConsumedFalse(user.getId(), VerificationPurpose.EMAIL_VERIFICATION)
                .stream()
                .max(Comparator.comparing(VerificationToken::getCreatedAt))
                .orElseThrow();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", verificationToken.getToken()))))
                .andExpect(status().isOk());

        return userRepository.findByEmail(email).orElseThrow();
    }

    private String loginAndGetAccessToken(String email, String password, String userAgent) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        MvcResult mvcResult = mockMvc.perform(post("/api/auth/login")
                        .header("User-Agent", userAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private void assertPublishedEvent(AuthEventType eventType, String aggregateId) {
        AuthEvent event = authEventRepository.findByEventTypeOrderByCreatedAtDesc(eventType)
                .stream()
                .filter(candidate -> candidate.getAggregateId().equals(aggregateId))
                .findFirst()
                .orElseThrow();

        assertNotNull(event.getPublishedAt());
    }
}
