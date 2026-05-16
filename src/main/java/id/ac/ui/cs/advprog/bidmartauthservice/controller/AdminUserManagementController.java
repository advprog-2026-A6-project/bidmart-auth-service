package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.DeactivateUserRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.service.AdminUserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:deactivate')")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId,
                                            @RequestBody(required = false) DeactivateUserRequest request,
                                            Principal principal) {
        String reason = request == null ? null : request.getReason();
        User user = adminUserManagementService.deactivateUser(userId, reason, principal.getName());

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "active", user.isActive(),
                "deactivatedAt", user.getDeactivatedAt(),
                "deactivationReason", user.getDeactivationReason()
        ));
    }
}
