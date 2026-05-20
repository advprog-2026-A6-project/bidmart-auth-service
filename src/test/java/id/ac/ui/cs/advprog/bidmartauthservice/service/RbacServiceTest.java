package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Permission;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthEventPublisherService authEventPublisherService;

    @InjectMocks
    private RbacService rbacService;

    private Permission bidPlace;
    private Permission auctionCreate;
    private Role buyerRole;
    private User user;

    @BeforeEach
    void setUp() {
        bidPlace = Permission.builder().id(1L).name("bid:place").build();
        auctionCreate = Permission.builder().id(2L).name("auction:create").build();
        buyerRole = Role.builder().id(1L).name("BUYER").permissions(new HashSet<>()).build();
        user = User.builder()
                .id(10L)
                .email("user@test.com")
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void createRoleNormalizesNameAndPublishesEvent() {
        when(roleRepository.findByName("SELLER")).thenReturn(Optional.empty());
        when(permissionRepository.findByName("auction:create")).thenReturn(Optional.of(auctionCreate));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role role = rbacService.createRole(" seller ", Set.of(" auction:create "));

        assertThat(role.getName()).isEqualTo("SELLER");
        assertThat(role.getPermissions()).containsExactly(auctionCreate);
        verify(authEventPublisherService).publish(eq(AuthEventType.ROLE_CREATED), eq("ROLE"), eq("SELLER"), any(Map.class));
    }

    @Test
    void createRoleThrowsWhenAlreadyExists() {
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(Role.builder().name("ADMIN").build()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rbacService.createRole(" admin ", Set.of())
        );

        assertThat(exception.getMessage()).isEqualTo("Role sudah ada");
    }

    @Test
    void createPermissionNormalizesNameAndPublishesEvent() {
        when(permissionRepository.findByName("bid:place")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Permission permission = rbacService.createPermission(" Bid:Place ");

        assertThat(permission.getName()).isEqualTo("bid:place");
        verify(authEventPublisherService).publish(eq(AuthEventType.PERMISSION_CREATED), eq("PERMISSION"), eq("bid:place"), any(Map.class));
    }

    @Test
    void createPermissionThrowsWhenAlreadyExists() {
        when(permissionRepository.findByName("profile:read")).thenReturn(Optional.of(Permission.builder().name("profile:read").build()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rbacService.createPermission(" profile:read ")
        );

        assertThat(exception.getMessage()).isEqualTo("Permission sudah ada");
    }

    @Test
    void assignPermissionToRoleAddsPermissionAndPublishesChange() {
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.of(buyerRole));
        when(permissionRepository.findByName("bid:place")).thenReturn(Optional.of(bidPlace));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role updatedRole = rbacService.assignPermissionToRole(" buyer ", " bid:place ");

        assertThat(updatedRole.getPermissions()).containsExactly(bidPlace);
        verify(authEventPublisherService).publish(eq(AuthEventType.ROLE_PERMISSION_CHANGED), eq("ROLE"), eq("BUYER"), any(Map.class));
    }

    @Test
    void revokePermissionFromRoleRemovesPermissionAndPublishesChange() {
        buyerRole.setPermissions(new HashSet<>(Set.of(bidPlace)));
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.of(buyerRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role updatedRole = rbacService.revokePermissionFromRole("buyer", " BID:PLACE ");

        assertThat(updatedRole.getPermissions()).isEmpty();
        verify(authEventPublisherService).publish(eq(AuthEventType.ROLE_PERMISSION_CHANGED), eq("ROLE"), eq("BUYER"), any(Map.class));
    }

    @Test
    void assignRoleToUserAddsRoleAndPublishesChange() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.of(buyerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = rbacService.assignRoleToUser(10L, " buyer ");

        assertThat(updatedUser.getRoles()).containsExactly(buyerRole);
        verify(authEventPublisherService).publish(eq(AuthEventType.USER_ROLE_CHANGED), eq("USER"), eq("10"), any(Map.class));
    }

    @Test
    void revokeRoleFromUserRemovesRoleAndPublishesChange() {
        user.setRoles(new HashSet<>(Set.of(buyerRole)));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = rbacService.revokeRoleFromUser(10L, " buyer ");

        assertThat(updatedUser.getRoles()).isEmpty();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(authEventPublisherService).publish(eq(AuthEventType.USER_ROLE_CHANGED), eq("USER"), eq("10"), captor.capture());
        assertThat(captor.getValue()).containsEntry("action", "REVOKED");
    }

    @Test
    void assignPermissionToRoleThrowsWhenRoleMissing() {
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rbacService.assignPermissionToRole("admin", "bid:place")
        );

        assertThat(exception.getMessage()).isEqualTo("Role tidak ditemukan");
    }

    @Test
    void assignRoleToUserThrowsWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> rbacService.assignRoleToUser(99L, "buyer")
        );

        assertThat(exception.getMessage()).isEqualTo("User tidak ditemukan");
    }
}
