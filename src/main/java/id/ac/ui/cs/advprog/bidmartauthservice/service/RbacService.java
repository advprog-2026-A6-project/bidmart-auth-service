package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.model.Permission;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public Role createRole(String roleName, Set<String> permissionNames) {
        String normalizedName = normalize(roleName);
        if (roleRepository.findByName(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Role sudah ada");
        }

        Set<Permission> permissions = new HashSet<>();
        if (permissionNames != null) {
            for (String permissionName : permissionNames) {
                permissions.add(findPermission(permissionName));
            }
        }

        return roleRepository.save(Role.builder()
                .name(normalizedName)
                .permissions(permissions)
                .build());
    }

    public Permission createPermission(String permissionName) {
        String normalizedName = normalize(permissionName).toLowerCase();
        if (permissionRepository.findByName(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Permission sudah ada");
        }

        return permissionRepository.save(Permission.builder().name(normalizedName).build());
    }

    public Role assignPermissionToRole(String roleName, String permissionName) {
        Role role = findRole(roleName);
        role.getPermissions().add(findPermission(permissionName));
        return roleRepository.save(role);
    }

    public Role revokePermissionFromRole(String roleName, String permissionName) {
        Role role = findRole(roleName);
        role.getPermissions().removeIf(permission -> permission.getName().equalsIgnoreCase(permissionName.trim()));
        return roleRepository.save(role);
    }

    public User assignRoleToUser(Long userId, String roleName) {
        User user = findUser(userId);
        user.getRoles().add(findRole(roleName));
        return userRepository.save(user);
    }

    public User revokeRoleFromUser(Long userId, String roleName) {
        User user = findUser(userId);
        user.getRoles().removeIf(role -> role.getName().equalsIgnoreCase(roleName.trim()));
        return userRepository.save(user);
    }

    private Role findRole(String roleName) {
        return roleRepository.findByName(normalize(roleName))
                .orElseThrow(() -> new IllegalArgumentException("Role tidak ditemukan"));
    }

    private Permission findPermission(String permissionName) {
        return permissionRepository.findByName(normalize(permissionName).toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Permission tidak ditemukan"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
