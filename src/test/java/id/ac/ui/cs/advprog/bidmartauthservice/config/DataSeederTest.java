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
        when(roleRepository.count()).thenReturn(0L);

        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Role adminRole = Role.builder().name("ADMIN").build();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        dataSeeder.run();

        verify(permissionRepository, times(3)).save(any(Permission.class));
        verify(roleRepository, times(3)).save(any(Role.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRunSeederWhenDataAlreadyExists() throws Exception {
        when(roleRepository.count()).thenReturn(3L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        dataSeeder.run();

        verify(permissionRepository, never()).save(any(Permission.class));
        verify(roleRepository, never()).save(any(Role.class));
        verify(userRepository, never()).save(any(User.class));
    }
}