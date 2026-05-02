package id.ac.ui.cs.advprog.bidmartauthservice.config;

import id.ac.ui.cs.advprog.bidmartauthservice.model.Permission;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            Permission createAuction = permissionRepository.save(Permission.builder().name("auction:create").build());
            Permission banUser = permissionRepository.save(Permission.builder().name("user:ban").build());
            Permission placeBid = permissionRepository.save(Permission.builder().name("bid:place").build());

            roleRepository.save(Role.builder().name("ADMIN").permissions(Set.of(createAuction, banUser, placeBid)).build());
            roleRepository.save(Role.builder().name("SELLER").permissions(Set.of(createAuction)).build());
            roleRepository.save(Role.builder().name("BUYER").permissions(Set.of(placeBid)).build());

            System.out.println("Data Seeder: Roles dan Permissions berhasil dimasukkan!");
        }

        if (userRepository.findByEmail("admin@bidmart.com").isEmpty()) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

            User adminUser = User.builder()
                    .email("admin@bidmart.com")
                    .password(passwordEncoder.encode("AdminBidmart123!"))
                    .name("Super Administrator")
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            userRepository.save(adminUser);
            System.out.println("Data Seeder: Akun Admin default berhasil dibuat! (Email: admin@bidmart.com)");
        }
    }
}