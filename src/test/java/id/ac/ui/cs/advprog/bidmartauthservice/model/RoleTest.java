package id.ac.ui.cs.advprog.bidmartauthservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RoleTest {

    private Role role;
    private Set<Permission> permissions;

    @BeforeEach
    void setUp() {
        permissions = new HashSet<>();
        permissions.add(Permission.builder().id(1L).name("READ_PRIVILEGE").build());

        role = Role.builder()
                .id(1L)
                .name("BUYER")
                .permissions(permissions)
                .build();
    }

    @Test
    void testRoleGetters() {
        assertEquals(1L, role.getId());
        assertEquals("BUYER", role.getName());
        assertNotNull(role.getPermissions());
        assertEquals(1, role.getPermissions().size());
    }

    @Test
    void testRoleSetters() {
        Role emptyRole = new Role();
        emptyRole.setId(2L);
        emptyRole.setName("SELLER");

        Set<Permission> newPermissions = new HashSet<>();
        emptyRole.setPermissions(newPermissions);

        assertEquals(2L, emptyRole.getId());
        assertEquals("SELLER", emptyRole.getName());
        assertEquals(newPermissions, emptyRole.getPermissions());
    }
}