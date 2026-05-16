package id.ac.ui.cs.advprog.bidmartauthservice.config;

import id.ac.ui.cs.advprog.bidmartauthservice.model.Permission;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void testRunSeederWhenDatabaseIsEmpty() throws Exception {
        when(permissionRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Role adminRole = Role.builder().name("ADMIN").permissions(new HashSet<>()).build();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("SELLER")).thenReturn(Optional.empty());
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        dataSeeder.run();

        verify(permissionRepository, times(9)).save(any(Permission.class));
        verify(roleRepository, times(3)).save(any(Role.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRunSeederWhenDataAlreadyExists() throws Exception {
        when(permissionRepository.findByName(anyString()))
                .thenAnswer(invocation -> Optional.of(Permission.builder().name(invocation.getArgument(0)).build()));
        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(Role.builder().name("ADMIN").permissions(new HashSet<>()).build()));
        when(roleRepository.findByName("SELLER"))
                .thenReturn(Optional.of(Role.builder().name("SELLER").permissions(new HashSet<>()).build()));
        when(roleRepository.findByName("BUYER"))
                .thenReturn(Optional.of(Role.builder().name("BUYER").permissions(new HashSet<>()).build()));

        User adminUser = User.builder()
                .email("admin@bidmart.com")
                .emailVerified(true)
                .roles(new HashSet<>(java.util.Set.of(Role.builder().name("ADMIN").permissions(new HashSet<>()).build())))
                .build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(adminUser));

        dataSeeder.run();

        verify(permissionRepository, never()).save(any(Permission.class));
        verify(roleRepository, times(3)).save(any(Role.class));
        verify(userRepository, never()).save(any(User.class));
    }
}
