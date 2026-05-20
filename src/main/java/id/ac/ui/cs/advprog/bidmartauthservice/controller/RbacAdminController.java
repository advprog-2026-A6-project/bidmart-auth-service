package id.ac.ui.cs.advprog.bidmartauthservice.controller;

import id.ac.ui.cs.advprog.bidmartauthservice.dto.CreatePermissionRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.dto.CreateRoleRequest;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Permission;
import id.ac.ui.cs.advprog.bidmartauthservice.model.Role;
import id.ac.ui.cs.advprog.bidmartauthservice.model.User;
import id.ac.ui.cs.advprog.bidmartauthservice.service.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('rbac:manage')")
public class RbacAdminController {

    private final RbacService rbacService;

    @PostMapping("/permissions")
    public ResponseEntity<?> createPermission(@RequestBody CreatePermissionRequest request) {
        Permission permission = rbacService.createPermission(request.getName());
        return ResponseEntity.ok(Map.of("name", permission.getName()));
    }

    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@RequestBody CreateRoleRequest request) {
        Role role = rbacService.createRole(request.getName(), request.getPermissions());
        return ResponseEntity.ok(Map.of(
                "name", role.getName(),
                "permissions", role.getPermissions().stream().map(Permission::getName).sorted().toList()
        ));
    }

    @PostMapping("/roles/{roleName}/permissions/{permissionName}")
    public ResponseEntity<?> assignPermissionToRole(@PathVariable String roleName, @PathVariable String permissionName) {
        Role role = rbacService.assignPermissionToRole(roleName, permissionName);
        return ResponseEntity.ok(Map.of(
                "name", role.getName(),
                "permissions", role.getPermissions().stream().map(Permission::getName).sorted().toList()
        ));
    }

    @DeleteMapping("/roles/{roleName}/permissions/{permissionName}")
    public ResponseEntity<?> revokePermissionFromRole(@PathVariable String roleName, @PathVariable String permissionName) {
        Role role = rbacService.revokePermissionFromRole(roleName, permissionName);
        return ResponseEntity.ok(Map.of(
                "name", role.getName(),
                "permissions", role.getPermissions().stream().map(Permission::getName).sorted().toList()
        ));
    }

    @PostMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<?> assignRoleToUser(@PathVariable Long userId, @PathVariable String roleName) {
        User user = rbacService.assignRoleToUser(userId, roleName);
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream().map(Role::getName).sorted().toList()
        ));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<?> revokeRoleFromUser(@PathVariable Long userId, @PathVariable String roleName) {
        User user = rbacService.revokeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "roles", user.getRoles().stream().map(Role::getName).sorted().toList()
        ));
    }
}
