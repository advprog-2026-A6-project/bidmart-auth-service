package id.ac.ui.cs.advprog.bidmartauthservice.config;

import id.ac.ui.cs.advprog.bidmartauthservice.model.Permission;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.TwoFactorMethod;
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
        Permission createAuction = ensurePermission("auction:create");
        Permission banUser = ensurePermission("user:ban");
        Permission placeBid = ensurePermission("bid:place");
        Permission profileRead = ensurePermission("profile:read");
        Permission profileUpdate = ensurePermission("profile:update");
        Permission profileTwoFactorManage = ensurePermission("profile:2fa:manage");
        Permission sessionRead = ensurePermission("session:self:read");
        Permission sessionRevoke = ensurePermission("session:self:revoke");
        Permission rbacManage = ensurePermission("rbac:manage");

        ensureRole("ADMIN", Set.of(
                createAuction, banUser, placeBid,
                profileRead, profileUpdate, profileTwoFactorManage,
                sessionRead, sessionRevoke, rbacManage
        ));
        ensureRole("SELLER", Set.of(
                createAuction,
                profileRead, profileUpdate, profileTwoFactorManage,
                sessionRead, sessionRevoke
        ));
        ensureRole("BUYER", Set.of(
                placeBid,
                profileRead, profileUpdate, profileTwoFactorManage,
                sessionRead, sessionRevoke
        ));

        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        User adminUser = userRepository.findByEmail("admin@bidmart.com").orElse(null);

        if (adminUser == null) {
            adminUser = User.builder()
                    .email("admin@bidmart.com")
                    .password(passwordEncoder.encode("AdminBidmart123!"))
                    .name("Super Administrator")
                    .emailVerified(true)
                    .twoFactorMethod(TwoFactorMethod.NONE)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            userRepository.save(adminUser);
            System.out.println("Data Seeder: Akun Admin default berhasil dibuat! (Email: admin@bidmart.com)");
            return;
        }

        boolean adminUpdated = false;
        if (!adminUser.isEmailVerified()) {
            adminUser.setEmailVerified(true);
            adminUpdated = true;
        }
        if (adminUser.getTwoFactorMethod() == null) {
            adminUser.setTwoFactorMethod(TwoFactorMethod.NONE);
            adminUpdated = true;
        }
        if (adminUser.getRoles() == null || adminUser.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getName()))) {
            adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
            adminUpdated = true;
        }
        if (adminUpdated) {
            userRepository.save(adminUser);
        }
    }

    private Permission ensurePermission(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(Permission.builder().name(name).build()));
    }

    private void ensureRole(String roleName, Set<Permission> permissions) {
        Role role = roleRepository.findByName(roleName)
                .orElse(Role.builder().name(roleName).permissions(new HashSet<>()).build());

        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
    }
}
