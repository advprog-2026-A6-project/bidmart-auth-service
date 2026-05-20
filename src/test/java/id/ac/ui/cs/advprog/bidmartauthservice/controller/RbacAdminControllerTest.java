package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.CreatePermissionRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.CreateRoleRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Permission;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.service.RbacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacAdminControllerTest {

    @Mock
    private RbacService rbacService;

    @InjectMocks
    private RbacAdminController rbacAdminController;

    private Permission permission;
    private Role role;
    private User user;

    @BeforeEach
    void setUp() {
        permission = Permission.builder().name("bid:place").build();
        role = Role.builder().name("BUYER").permissions(new HashSet<>(Set.of(permission))).build();
        user = User.builder().id(1L).email("user@test.com").roles(new HashSet<>(Set.of(role))).build();
    }

    @Test
    void createPermissionReturnsPermissionName() {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("bid:place");
        when(rbacService.createPermission("bid:place")).thenReturn(permission);

        ResponseEntity<?> response = rbacAdminController.createPermission(request);

        assertThat(response.getBody()).isEqualTo(Map.of("name", "bid:place"));
    }

    @Test
    void createRoleReturnsSortedPermissions() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("buyer");
        request.setPermissions(Set.of("bid:place"));
        when(rbacService.createRole("buyer", Set.of("bid:place"))).thenReturn(role);

        ResponseEntity<?> response = rbacAdminController.createRole(request);

        assertThat(response.getBody()).isEqualTo(Map.of("name", "BUYER", "permissions", List.of("bid:place")));
    }

    @Test
    void assignPermissionToRoleReturnsUpdatedRole() {
        when(rbacService.assignPermissionToRole("BUYER", "bid:place")).thenReturn(role);

        ResponseEntity<?> response = rbacAdminController.assignPermissionToRole("BUYER", "bid:place");

        assertThat(response.getBody()).isEqualTo(Map.of("name", "BUYER", "permissions", List.of("bid:place")));
    }

    @Test
    void revokePermissionFromRoleReturnsUpdatedRole() {
        when(rbacService.revokePermissionFromRole("BUYER", "bid:place")).thenReturn(role);

        ResponseEntity<?> response = rbacAdminController.revokePermissionFromRole("BUYER", "bid:place");

        assertThat(response.getBody()).isEqualTo(Map.of("name", "BUYER", "permissions", List.of("bid:place")));
    }

    @Test
    void assignRoleToUserReturnsUpdatedUser() {
        when(rbacService.assignRoleToUser(1L, "BUYER")).thenReturn(user);

        ResponseEntity<?> response = rbacAdminController.assignRoleToUser(1L, "BUYER");

        assertThat(response.getBody()).isEqualTo(Map.of("userId", 1L, "email", "user@test.com", "roles", List.of("BUYER")));
    }

    @Test
    void revokeRoleFromUserReturnsUpdatedUser() {
        when(rbacService.revokeRoleFromUser(1L, "BUYER")).thenReturn(user);

        ResponseEntity<?> response = rbacAdminController.revokeRoleFromUser(1L, "BUYER");

        assertThat(response.getBody()).isEqualTo(Map.of("userId", 1L, "email", "user@test.com", "roles", List.of("BUYER")));
    }
}
