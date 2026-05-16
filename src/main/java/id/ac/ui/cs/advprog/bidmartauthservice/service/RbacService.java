package id.ac.ui.cs.advprog.bidmartauthservice.service;

import id.ac.ui.cs.advprog.bidmartauthservice.model.Permission;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEventType;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.RoleRepository;
import id.ac.ui.cs.advprog.bidmartauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuthEventPublisherService authEventPublisherService;

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

        Role role = roleRepository.save(Role.builder()
                .name(normalizedName)
                .permissions(permissions)
                .build());

        authEventPublisherService.publish(
                AuthEventType.ROLE_CREATED,
                "ROLE",
                role.getName(),
                Map.of(
                        "roleName", role.getName(),
                        "permissions", role.getPermissions().stream().map(Permission::getName).sorted().toList()
                )
        );

        return role;
    }

    public Permission createPermission(String permissionName) {
        String normalizedName = normalize(permissionName).toLowerCase();
        if (permissionRepository.findByName(normalizedName).isPresent()) {
            throw new IllegalArgumentException("Permission sudah ada");
        }

        Permission permission = permissionRepository.save(Permission.builder().name(normalizedName).build());

        authEventPublisherService.publish(
                AuthEventType.PERMISSION_CREATED,
                "PERMISSION",
                permission.getName(),
                Map.of("permissionName", permission.getName())
        );

        return permission;
    }

    public Role assignPermissionToRole(String roleName, String permissionName) {
        Role role = findRole(roleName);
        role.getPermissions().add(findPermission(permissionName));
        Role savedRole = roleRepository.save(role);
        publishRolePermissionChange("ASSIGNED", savedRole, permissionName);
        return savedRole;
    }

    public Role revokePermissionFromRole(String roleName, String permissionName) {
        Role role = findRole(roleName);
        role.getPermissions().removeIf(permission -> permission.getName().equalsIgnoreCase(permissionName.trim()));
        Role savedRole = roleRepository.save(role);
        publishRolePermissionChange("REVOKED", savedRole, permissionName);
        return savedRole;
    }

    public User assignRoleToUser(Long userId, String roleName) {
        User user = findUser(userId);
        user.getRoles().add(findRole(roleName));
        User savedUser = userRepository.save(user);
        publishUserRoleChange("ASSIGNED", savedUser, roleName);
        return savedUser;
    }

    public User revokeRoleFromUser(Long userId, String roleName) {
        User user = findUser(userId);
        user.getRoles().removeIf(role -> role.getName().equalsIgnoreCase(roleName.trim()));
        User savedUser = userRepository.save(user);
        publishUserRoleChange("REVOKED", savedUser, roleName);
        return savedUser;
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

    private void publishRolePermissionChange(String action, Role role, String permissionName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("roleName", role.getName());
        payload.put("permissionName", normalize(permissionName).toLowerCase());
        payload.put("currentPermissions", role.getPermissions().stream().map(Permission::getName).sorted().toList());

        authEventPublisherService.publish(
                AuthEventType.ROLE_PERMISSION_CHANGED,
                "ROLE",
                role.getName(),
                payload
        );
    }

    private void publishUserRoleChange(String action, User user, String roleName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("userId", user.getId());
        payload.put("email", user.getEmail());
        payload.put("roleName", normalize(roleName));
        payload.put("currentRoles", user.getRoles().stream().map(Role::getName).sorted().toList());

        authEventPublisherService.publish(
                AuthEventType.USER_ROLE_CHANGED,
                "USER",
                user.getId().toString(),
                payload
        );
    }
}
