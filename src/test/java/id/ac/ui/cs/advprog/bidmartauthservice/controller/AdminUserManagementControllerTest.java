package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.DeactivateUserRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.service.AdminUserManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementControllerTest {

    @Mock
    private AdminUserManagementService adminUserManagementService;

    @InjectMocks
    private AdminUserManagementController adminUserManagementController;

    @Test
    void deactivateUserUsesProvidedReason() {
        DeactivateUserRequest request = new DeactivateUserRequest();
        request.setReason("Spam");
        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .active(false)
                .deactivatedAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                .deactivationReason("Spam")
                .build();
        Principal principal = () -> "admin@test.com";
        when(adminUserManagementService.deactivateUser(1L, "Spam", "admin@test.com")).thenReturn(user);

        ResponseEntity<?> response = adminUserManagementController.deactivateUser(1L, request, principal);

        assertThat(response.getBody()).isEqualTo(Map.of(
                "userId", 1L,
                "email", "user@test.com",
                "active", false,
                "deactivatedAt", LocalDateTime.of(2026, 5, 20, 10, 0),
                "deactivationReason", "Spam"
        ));
    }

    @Test
    void deactivateUserAllowsNullRequest() {
        User user = User.builder()
                .id(2L)
                .email("user2@test.com")
                .active(false)
                .deactivatedAt(LocalDateTime.of(2026, 5, 20, 11, 0))
                .deactivationReason("Dinonaktifkan oleh administrator")
                .build();
        Principal principal = () -> "admin@test.com";
        when(adminUserManagementService.deactivateUser(2L, null, "admin@test.com")).thenReturn(user);

        ResponseEntity<?> response = adminUserManagementController.deactivateUser(2L, null, principal);

        assertThat(response.getBody()).isEqualTo(Map.of(
                "userId", 2L,
                "email", "user2@test.com",
                "active", false,
                "deactivatedAt", LocalDateTime.of(2026, 5, 20, 11, 0),
                "deactivationReason", "Dinonaktifkan oleh administrator"
        ));
    }
}
